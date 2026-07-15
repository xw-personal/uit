package com.uit.agentcore.tools;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.microsoft.playwright.Page;
import com.uit.api.common.RestClientHolder;

public class ImgAnalyzeTool {
    private RestClient restClient = RestClientHolder.getRestClient();
    private final Page page;

    public ImgAnalyzeTool(Page page) {
        this.page = page;
    }

    public String imgAnalyze(String xpath){
        byte[] screenshotBytes = this.page.locator(xpath).screenshot();
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
        return response;
    }
}
