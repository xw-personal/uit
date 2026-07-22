package com.uit.agentcore.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.BoundingBox;
import com.uit.api.common.RestClientHolder;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

public class ImgAnalyzeTool {
    private RestClient restClient = RestClientHolder.getRestClient();
    private final Page page;

    public ImgAnalyzeTool(Page page) {
        this.page = page;
    }

    @Tool("截取页面(或指定 xpath 区域)截图,识别可交互元素,并返回每个元素的 "
      + "tag/text/xpath/css(已通过 DOM 精确换算,可直接用于 BrowserUseTool 的 selector)。"
      + "坐标换算(DPR、截图偏移、滚动)在工具内部完成,返回的 selector 可直接用。"
      + "返回 {elements:[{type,confidence,center,tag,text,xpath,css}], imageAnnotated}.")
    public InnerImgAnalyzeTool imgAnalyze(@P("要截取分析的元素 xpath;为空则截整页") String xpath){
        try {
            byte[] screenshotBytes = {};
            Double offsetX = 0.0;
            Double offsetY = 0.0;
            if (xpath.isEmpty() || xpath == "null"){
                screenshotBytes = this.page.screenshot();
            }else{
                Locator locator = this.page.locator(xpath);
                screenshotBytes = locator.screenshot();
                BoundingBox boundingBox = locator.boundingBox();
                if (boundingBox != null){
                    offsetX = boundingBox.x;
                    offsetY = boundingBox.y;
                }
            }
            
            MultipartBodyBuilder builder = new MultipartBodyBuilder();

            // 将字节数组包装为 ByteArrayResource，并指定文件名
            ByteArrayResource resource = new ByteArrayResource(screenshotBytes) {
                @Override
                public String getFilename() {
                    return "screenshot.png"; // 可动态生成文件名
                }
            };

            // 添加文件部分，字段名必须匹配 FastAPI 接口参数名（如 "file"）
            builder.part("file", resource, MediaType.IMAGE_PNG);

            String response = restClient.post()
                    .uri("/analyze")
                    .body(builder.build())
                    .retrieve()
                    .body(String.class);
            
            ObjectMapper objectMapper = new ObjectMapper();
            ImgAnalyzeResult imgAnalyzeResult = objectMapper.readValue(response, ImgAnalyzeResult.class);
            List<VisualElement> visualElements = new ArrayList<>();
            for (Element element : imgAnalyzeResult.elements()) {
                Double[] viewport = toViewport(element.center(), offsetX, offsetY, offsetY, false, page);
                String js = """
                    ([x, y]) => {
                    const stack = document.elementsFromPoint(x, y);
                    let el = stack[0];
                    for (const c of stack) {
                        if ((c.innerText && c.innerText.trim()) || ['INPUT','BUTTON','A','SELECT'].includes(c.tagName)) {
                        el = c; break;
                        }
                    }
                    if (!el) return null;

                    const tag = el.tagName.toLowerCase();
                    const text = (el.innerText || '').trim().slice(0, 100);

                    // xpath:优先语义,兜底绝对路径(含 /html)
                    let xpath;
                    if (el.id) xpath = `//*[@id="${el.id}"]`;
                    else if (el.name) xpath = `//${tag}[@name="${el.name}"]`;
                    else if (el.placeholder) xpath = `//${tag}[@placeholder="${el.placeholder}"]`;
                    else if (el.type) xpath = `//${tag}[@type="${el.type}"]`;
                    else {
                        const parts = [];
                        let n = el;
                        while (n && n.nodeType === 1) {
                        let idx = 1, sib = n.previousElementSibling;
                        while (sib) { if (sib.tagName === n.tagName) idx++; sib = sib.previousElementSibling; }
                        const t = n.tagName.toLowerCase();
                        parts.unshift(n === document.documentElement ? t : `${t}[${idx}]`);
                        n = n.parentElement;
                        }
                        xpath = '/' + parts.join('/');
                    }

                    // css:优先 id,否则路径
                    let css;
                    if (el.id) css = '#' + CSS.escape(el.id);
                    else {
                        const parts = [];
                        let n = el;
                        while (n && n.nodeType === 1 && n !== document.documentElement) {
                        let sel = n.tagName.toLowerCase();
                        if (n.parentElement) {
                            const same = [...n.parentElement.children].filter(c => c.tagName === n.tagName);
                            if (same.length > 1) sel += `:nth-child(${[...n.parentElement.children].indexOf(n) + 1})`;
                        }
                        parts.unshift(sel);
                        n = n.parentElement;
                        }
                        css = parts.join(' > ');
                    }

                    return { tag, text, xpath, css };
                    }
                    """;
                Map<String, Object> dom  = (Map<String, Object>) page.evaluate(js, viewport);
                if (dom == null) {
                    // 坐标在视口外 / 命中 Canvas 无 DOM -> 回退 xy: 坐标点击
                    
                }
                visualElements.add(
                    new VisualElement(
                        String.valueOf(dom.get("tag")),
                        String.valueOf(dom.get("text")),
                        String.valueOf(dom.get("xpath")),
                        String.valueOf(dom.get("css"))
                    )
                );
            }
            return new InnerImgAnalyzeTool(imgAnalyzeResult, visualElements,"");
        } catch (Exception e) {
            return InnerImgAnalyzeTool.error(e.getMessage());
        }
    }

    private Double[] toViewport(Double[] center, Double offsetX, Double offsetY,Double dpr, boolean isFullPage,Page page){
        Double vx = center[0] / dpr + offsetX;
        Double vy = center[1] / dpr + offsetY;
        if (isFullPage) {
            // full-page 截图:截图 y 是页面绝对坐标,减 scrollY 转回视口坐标
           page.evaluate("([x, y]) => { window.scrollTo(x - window.innerWidth/2, y - window.innerHeight/2); }", new Double[]{vx, vy});
        }
        return new Double[]{vx, vy};
    }

    public record InnerImgAnalyzeTool(
        ImgAnalyzeResult imgAnalyzeResult,
        List<VisualElement> visualElements,
        String error
    ) {
        public static InnerImgAnalyzeTool error(String msg) {
          return new InnerImgAnalyzeTool(null,List.of(), msg);
        }
    }
    
    public record ImgAnalyzeResult(
        PageSize pageSize,
        Integer elementsCount,
        List<Element> elements
    ) {}

    public record Element(
        String type,
        Double confidence,
        Double[] bbox,
        Double[] center

    ) {}
    public record VisualElement(
        String tag,             // ★ DOM tag(新增,elementFromPoint)
        String text,            // ★ DOM 文本
        String xpath,           // ★ 可直接用
        String css              // ★ 可直接用
    ){}

    public record PageSize(
        Integer width,
        Integer height
    ) {}
}
