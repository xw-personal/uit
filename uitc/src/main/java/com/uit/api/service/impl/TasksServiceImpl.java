package com.uit.api.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.ibatis.javassist.bytecode.analysis.Analyzer;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Frame.GetByRoleOptions;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.ScreenshotType;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.uit.agentcore.agents.ExploerAnalyzer;
import com.uit.agentcore.agents.ExploerAnalyzer.ElementRef;
import com.uit.agentcore.agents.ExploerAnalyzer.LoginAnalysis;
import com.uit.api.entry.LoginUser;
import com.uit.api.entry.Result;
import com.uit.api.entry.UrlElements;
import com.uit.api.entry.UrlElements.UIComponent;
import com.uit.api.entry.User;
import com.uit.api.handler.BusinessException;
import com.uit.api.service.TasksService;
import com.uit.api.utils.BrowserOperation;
import com.uit.api.utils.UserContext;
import com.uit.api.vo.LoginStatus;
import com.uit.api.websocket.WebsocketService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TasksServiceImpl implements TasksService{

    private final ExploerAnalyzer analyzer;
    private final WebsocketService websocketService;

    public TasksServiceImpl(ExploerAnalyzer analyzer,WebsocketService websocketService) {
        this.analyzer = analyzer;
        this.websocketService = websocketService;
    }


    @Override
    public void processTask(LoginUser user) {
        // Implement the logic to process the task here
        Playwright playwright = Playwright.create();
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(false)      // 显示浏览器窗口
                .setChannel("chrome")    // 使用 Chrome
                .setSlowMo(500);   
        Browser browser = playwright.chromium().launch(options);
        //创建独立的上下文，并设置 setIgnoreHTTPSErrors(true)：忽略 HTTPS 证书错误
        BrowserContext context = browser.newContext(
                new Browser.NewContextOptions().setIgnoreHTTPSErrors(true)
                );
        BrowserOperation.login(user, context);
        
    }

    @Override
    public void login(String url) {
        String taskId = UUID.randomUUID().toString();
        log.info("开始处理任务，taskId: " + taskId);
        CompletableFuture.runAsync(() ->{
            CompletableFuture<LoginUser> future = new CompletableFuture<>();
            try (Playwright playwright = Playwright.create()){
                BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                    .setHeadless(true)      // 隐藏浏览器窗口
                    .setChannel("chrome")    // 使用 Chrome
                    .setSlowMo(100);
                Browser browser = playwright.chromium().launch(options);
                //创建独立的上下文，并设置 setIgnoreHTTPSErrors(true)：忽略 HTTPS 证书错误
                BrowserContext context = browser.newContext(
                        new Browser.NewContextOptions().setIgnoreHTTPSErrors(true)
                        );
                Page page = context.newPage();
                page.navigate(url);
                page.waitForLoadState(LoadState.NETWORKIDLE);
                String elementsJson = this.getElements(page);
                log.info("开始分析登录页...");
                LoginAnalysis loginAnalysis = analyzer.analyze(elementsJson);
                String userId = UserContext.getUser().getId();
                log.info("登录页分析结果："+loginAnalysis);
                UrlElements urlElement = new UrlElements();
                urlElement.setTaskId(taskId);
                switch (loginAnalysis.loginType()) {
                    case PASSWORD_ONLY:                       
                        urlElement.setType("PASSWORD_ONLY");
                        urlElement.setComponents(
                            List.of(
                                new UIComponent("account","input"),
                                new UIComponent("password","input")
                            )
                        );
                        websocketService.pushAnalysisResult(userId, urlElement);
                        break;
                    case PASSWORD_CAPTCHA:
                        String png = this.captchaImageByXpath(page,loginAnalysis.fields().captchaImage());
                        urlElement.setType("PASSWORD_CAPTCHA");                      
                        urlElement.setComponents(
                            List.of(
                                new UIComponent("account","input"),
                                new UIComponent("password","input"),
                                new UIComponent("captcha","captcha",Map.of(
                                    "captchaType", "TEXT_IMAGE",
                                    "captchaImage", png
                                ))
                            )
                        );
                        websocketService.pushAnalysisResult(userId, urlElement);
                        break;
                    case SMS:
                        break;
                    case QRCODE:
                        
                        break;
                    case SLIDER:
                        
                        break;
                    default:
                        break;
                }
                LoginUser users = new LoginUser();
                try {
                    users = future.get();
                } catch (Exception e) {
                    // TODO: handle exception
                }
                users.setUrl(url);
                loginAnalysis.loginType().login(page, loginAnalysis.fields(), users);
            }
            
        });
        

        // List<Locator> textboxes = page.getByRole(AriaRole.TEXTBOX).all();
        // for (Locator locator : textboxes) {
        //     String placeholder = locator.getAttribute("placeholder");
        //     new GetByRoleOptions().setName(placeholder);
            
        // }
        // if (page.url().equals(user.getUrl())){
        //     System.out.println("登录失败，请检测账号密码是否正确，或关闭登录验证码");
        // }
        // context.storageState(new BrowserContext.StorageStateOptions().setPath(Path.of("auth.json")));
    }

    public LoginStatus loginStatus() {
        // Implement the logic to check login status here
        // For example, you can use Playwright to navigate to a page that requires authentication and check if the user is logged in
        // Return a LoginStatus object indicating the login status
        LoginStatus loginStatus = new LoginStatus();
        try {
            Path authPath = Paths.get("auth.json");
            String authData = Files.readString(authPath);
            JSONObject jsonObject = new JSONObject(authData);
            jsonObject.getJSONArray("origins").forEach(orgin->{
                JSONObject origin = (JSONObject) orgin;
                origin.getJSONArray("localStorage").forEach(local -> {
                    JSONObject localStorage = (JSONObject) local;
                    if (localStorage.getString("name").equals("refresh_expire_time")){
                        String expires = localStorage.getString("value");
                        long timestamp = Long.parseLong(expires);
                        // 自动判断：如果长度 >= 13 视为毫秒，否则视为秒
                        Instant instant = expires.length() >= 13
                                ? Instant.ofEpochMilli(timestamp)
                                : Instant.ofEpochSecond(timestamp);
                        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                        if (dateTime.isBefore(LocalDateTime.now())){
                            loginStatus.setExpireTime("登录到期，请重新登录");
                            loginStatus.setStatus((byte) 0);
                        }else{
                            String expireTime = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                            loginStatus.setExpireTime(expireTime);
                            loginStatus.setStatus((byte) 1);
                        }
                        
                    }
                });
            });
        } catch (NoSuchFileException  e) {
            // Handle the case where the auth.json file does not exist
            loginStatus.setExpireTime("登录到期，请重新登录");
            loginStatus.setStatus((byte) 0);
        } catch (IOException e) {
            // Handle other I/O exceptions
            throw new BusinessException(500, "检查登录状态时发生错误");
        }
        return loginStatus;
    }

    private String getElements(Page page) {
        String js = """
                        () => {
                        // —— 辅助函数必须定义在这里 ——
                        const getXPath = (el) => {
                                if (el.nodeType !== 1) return '';
                                if (el.id) return `//*[@id="${el.id}"]`;
                                const parts = [];
                                let node = el;
                                while (node && node.nodeType === 1 && node !== document.documentElement) {
                                let idx = 1, sib = node.previousElementSibling;
                                while (sib) {
                                if (sib.tagName === node.tagName) idx++;
                                sib = sib.previousElementSibling;
                                }
                                parts.unshift(`${node.tagName.toLowerCase()}[${idx}]`);
                                node = node.parentElement;
                                }
                                return '/' + parts.join('/');
                        };
                        const getCss = (el) => {   // 顺带给个 CSS 路径,比 xpath 更稳
                                if (el.id) return '#' + CSS.escape(el.id);
                                const parts = [];
                                let node = el;
                                while (node && node.nodeType === 1 && node !== document.documentElement) {
                                let sel = node.tagName.toLowerCase();
                                if (node.parentElement) {
                                const same = [...node.parentElement.children].filter(c => c.tagName === node.tagName);
                                if (same.length > 1) sel += `:nth-child(${[...node.parentElement.children].indexOf(node)+1})`;
                                }
                                parts.unshift(sel);
                                node = node.parentElement;
                                }
                                return parts.join(' > ');
                        };

                        return [...document.querySelectorAll(
                                'input, button, a, img, canvas, [role=button], [role=img], textarea, select')]
                                .filter(e => { const r = e.getBoundingClientRect(); return r.width > 0 && r.height > 0; })
                                .map(e => ({
                                tag: e.tagName.toLowerCase(),
                                type: e.type || null,
                                id: e.id || null,
                                name: e.name || null,
                                className: typeof e.className === 'string' ? e.className : null,
                                placeholder: e.placeholder || null,
                                alt: e.alt || null,
                                src: (e.src || '').slice(0, 80),
                                text: (e.innerText || '').trim().slice(0, 30) || null,
                                ariaLabel: e.getAttribute('aria-label') || null,
                                role: e.getAttribute('role') || null,
                                xpath: getXPath(e),
                                css: getCss(e)
                                }));
                        }
                        """;
        List<Map<String,Object>> candidates = (List<Map<String,Object>>) page.evaluate(js);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String json = objectMapper.writeValueAsString(candidates);
            return json;
        } catch (JsonProcessingException e) {
            // TODO: handle exception
            e.printStackTrace();
            return "";
        }
        
    }

    private String captchaImageByXpath(Page page,ElementRef captchaImage){
        Locator locator = page.locator("xpath=" + captchaImage.xpath());
        locator.waitFor(new Locator.WaitForOptions()
              .setState(WaitForSelectorState.VISIBLE)
              .setTimeout(8000));
        try {
            Object tag = locator.evaluate("el => el.tagName");
            if ("IMG".equalsIgnoreCase(String.valueOf(tag))) {
                page.waitForFunction("el => el.complete && el.naturalWidth > 0", locator,
                        new Page.WaitForFunctionOptions().setTimeout(8000));
            }
        } catch (Exception ignore) {
            // 非 img 元素或超时,继续走截图
        }
        byte[] png = locator.screenshot(new Locator.ScreenshotOptions().setType(ScreenshotType.PNG));      
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(png);
    }
}
