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
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.ScreenshotType;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.uit.agentcore.AgentService;
import com.uit.agentcore.agents.ExploerAnalyzer;
import com.uit.agentcore.agents.SequenceAgents;
import com.uit.agentcore.agents.ExploerAnalyzer.ElementRef;
import com.uit.agentcore.agents.ExploerAnalyzer.LoginAnalysis;
import com.uit.agentcore.tools.PageEvaluateTool;
import com.uit.agentcore.tools.PlaywrightAoyaExecutionEngine;
import com.uit.agentcore.tools.ImgAnalyzeTool;
import com.uit.agentcore.tools.ImgAnalyzeTool.ImgAnalyzeResult;
import com.uit.api.common.AgentProgressListenerFactory;
import com.uit.api.common.LoginType;
import com.uit.api.common.TaskPendingManager;
import com.uit.api.entry.LoginUser;
import com.uit.api.entry.UrlElements;
import com.uit.api.entry.UrlElements.UIComponent;
import com.uit.api.handler.BusinessException;
import com.uit.api.service.TasksService;
import com.uit.api.utils.BrowserUtils;
import com.uit.api.utils.RedisKeyPrefix;
import com.uit.api.utils.UserContext;
import com.uit.api.vo.LoginStatus;
import com.uit.api.websocket.WebsocketService;

import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.community.tool.browseruse.BrowserUseTool;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Sinks;

@Slf4j
@Service
public class TasksServiceImpl implements TasksService{

    private final ChatModel chatModel;
    private final WebsocketService websocketService;
    private final RedisTemplate<String,Object> redisTemplate;
    private final TaskPendingManager taskPendingManager;
    private final AgentProgressListenerFactory listenerFactory;

    public TasksServiceImpl(WebsocketService websocketService,
                            RedisTemplate<String,Object> redisTemplate,
                            TaskPendingManager taskPendingManager,
                            ChatModel chatModel,
                            AgentProgressListenerFactory listenerFactory) {
        this.websocketService = websocketService;
        this.redisTemplate = redisTemplate;
        this.taskPendingManager = taskPendingManager;
        this.listenerFactory = listenerFactory;
        this.chatModel = chatModel;
    }


    @Override
    public void processTask(String msg, Sinks.Many<Object> sink) {
        String userId = UserContext.getUser().getId();
        CompletableFuture.runAsync(()->{
            try(Playwright playwright = Playwright.create()){
                BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                        .setHeadless(false)      // 显示浏览器窗口
                        .setChannel("chrome")    // 使用 Chrome
                        .setSlowMo(100);   
                Browser browser = playwright.chromium().launch(options);
                // String auth = (String)redisTemplate.opsForValue().get(RedisKeyPrefix.LOGIN_AUTH + userId);
                //创建独立的上下文，并设置 setIgnoreHTTPSErrors(true)：忽略 HTTPS 证书错误
                BrowserContext context = browser.newContext(
                        new Browser.NewContextOptions().setIgnoreHTTPSErrors(true)//.setStorageState(auth)
                        );
                PlaywrightAoyaExecutionEngine engine = PlaywrightAoyaExecutionEngine.builder().browser(context).build();
                Page page = engine.getPage();
                BrowserUseTool browserUseTool = BrowserUseTool.from(
                    engine
                );
                PageEvaluateTool pageEvaluateTool = new PageEvaluateTool(page);

                ImgAnalyzeTool imgAnalyzeTool = new ImgAnalyzeTool(page);
                Map<String,List<Object>> tools = Map.of("UIExplorerTools",List.of(browserUseTool,pageEvaluateTool,imgAnalyzeTool));
                try {
                    AgentListener listener = listenerFactory.forSink(userId, sink);
                    SequenceAgents agents = AgentService.SequenceAgentServiceBuilder(chatModel, listener,tools);
                    String result = agents.Sequence(msg);
                    sink.tryEmitNext(Map.of("phase", "finished", "result", result));
                } catch (Exception e) {
                    sink.tryEmitNext(Map.of("phase", "error", "message", e.getMessage()));
                } finally{
                    sink.tryEmitComplete();
                }   
            }catch(Exception e){
                log.error("浏览器初始化失败：" + e);
            }
        });
    }

    @Override
    public void analyzer(String url) {
        String taskId = UUID.randomUUID().toString();
        
        log.info("开始处理任务，taskId: " + taskId);
        String userId = UserContext.getUser().getId();
        CompletableFuture.runAsync(() ->{
            try (Playwright playwright = Playwright.create()){
                BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                    .setHeadless(false)      // 隐藏浏览器窗口
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
                LoginAnalysis loginAnalysis =  AgentService.analyzer(chatModel).analyze(elementsJson);
                
                log.info("登录页分析结果："+loginAnalysis);
                UrlElements<List<UIComponent>> urlElement = new UrlElements<>();
                urlElement.setTaskId(taskId);
                redisTemplate.opsForHash().put(RedisKeyPrefix.TASK_STATE + taskId, "LoginType", loginAnalysis.loginType());
                redisTemplate.opsForHash().put(RedisKeyPrefix.TASK_STATE + taskId, "status", 1);
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
                        String png = BrowserUtils.captchaImageByXpath(page,loginAnalysis.fields().captchaImage());
                        log.info("[CAPTCHA] 截图时 src={}", png);
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
                        urlElement.setType("SMS");
                        urlElement.setComponents(
                            List.of(
                                new UIComponent("account","input"),
                                new UIComponent("password","input")
                            )
                        );
                        websocketService.pushAnalysisResult(userId, urlElement);
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
                    UrlElements<Map<String,Object>> urlElementStatus = new UrlElements<>();
                    while (true) {
                        CompletableFuture<LoginUser> future = new CompletableFuture<>();
                        taskPendingManager.put(taskId, future);
                        users = future.get();
                        users.setUrl(url);
                        String msg = loginAnalysis.loginType().login(page, loginAnalysis.fields(), users);

                        if (msg.contains("成功")) break;
                        urlElementStatus.setType("LOGIN_STATUS");
                        urlElementStatus.setComponents(Map.of(
                            "status",false,
                            "message",msg,
                            "data", loginAnalysis.loginType() == LoginType.PASSWORD_CAPTCHA?
                                    Map.of(
                                        "captchaType", "TEXT_IMAGE",
                                        "captchaImage",BrowserUtils.captchaImageByXpath(page,loginAnalysis.fields().captchaImage())
                                    ):
                                    ""
                        ));
                        websocketService.pushAnalysisResult(userId, urlElementStatus);
                    }
                    taskPendingManager.remove(taskId);
                    String stateJson = context.storageState();
                    redisTemplate.opsForValue().set(RedisKeyPrefix.LOGIN_AUTH + userId, stateJson);
                    redisTemplate.opsForHash().put(RedisKeyPrefix.TASK_STATE + taskId, "status", 0);
                    urlElementStatus.setType("LOGIN_STATUS");
                    urlElementStatus.setComponents(Map.of(
                        "status",true,
                        "message","登录成功"
                    ));
                    websocketService.pushAnalysisResult(userId, urlElementStatus);
                } catch (Exception e) {
                    // TODO: handle exception
                }
                
            }catch (Exception e) {
                log.error("异步任务执行失败", e);
            }
            
        });
    }

    public LoginStatus loginStatus() {
        // Implement the logic to check login status here
        // For example, you can use Playwright to navigate to a page that requires authentication and check if the user is logged in
        // Return a LoginStatus object indicating the login status
        LoginStatus loginStatus = new LoginStatus();
        String userId = UserContext.getUser().getId();
        try {
            Path authPath = Paths.get(userId+"_auth.json");
            String authData = Files.readString(authPath);
            JsonNode root = new ObjectMapper().readTree(authData);
            for (JsonNode origin : root.path("origins")) {
                loginStatus.setUrl(origin.path("origin").asText());
                for (JsonNode ls : origin.path("localStorage")) {
                    if ("refresh_expire_time".equals(ls.path("name").asText())) {
                        String expires = ls.path("value").asText();
                        long timestamp = Long.parseLong(expires);
                        // 自动判断：如果长度 >= 13 视为毫秒，否则视为秒
                        Instant instant = expires.length() >= 13
                                ? Instant.ofEpochMilli(timestamp)
                                : Instant.ofEpochSecond(timestamp);
                        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                        if (dateTime.isBefore(LocalDateTime.now())) {
                            loginStatus.setExpireTime("登录到期，请重新登录");
                            loginStatus.setStatus((byte) 0);
                        } else {
                            String expireTime = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                            loginStatus.setExpireTime(expireTime);
                            loginStatus.setStatus((byte) 1);
                        }
                    }
                }
            }
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
                                const tag = el.tagName.toLowerCase();
                                // 优先语义定位，抗页面结构变化；最后兜底绝对路径（含 /html 前缀）
                                if (el.id) return `//*[@id="${el.id}"]`;
                                if (el.name) return `//${tag}[@name="${el.name}"]`;
                                if (el.placeholder) return `//${tag}[@placeholder="${el.placeholder}"]`;
                                if (el.type) return `//${tag}[@type="${el.type}"]`;
                                const parts = [];
                                let node = el;
                                while (node && node.nodeType === 1) {   // 走到 <html>，保证绝对路径以 /html 开头
                                let idx = 1, sib = node.previousElementSibling;
                                while (sib) {
                                if (sib.tagName === node.tagName) idx++;
                                sib = sib.previousElementSibling;
                                }
                                const t = node.tagName.toLowerCase();
                                parts.unshift(node === document.documentElement ? t : `${t}[${idx}]`);
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
    

}
