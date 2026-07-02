package com.uit.uitc;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.RequestOptions;
import com.uit.agentcore.tools.PlaywrightAoyaExecutionEngine;

import dev.langchain4j.community.browser.playwright.PlaywrightBrowserExecutionEngine;
import dev.langchain4j.community.tool.browseruse.BrowserUseTool;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;



@SpringBootTest
class UitcApplicationTests {

	// @BeforeAll
	// static void init() {
	// 	// 选择使用 Spring RestClient
	// 	System.setProperty("langchain4j.http.clientBuilderFactory", 
	// 		"dev.langchain4j.http.client.spring.restclient.SpringRestClientBuilderFactory");
	// 	// 或者选择 JDK HttpClient
	// 	// System.setProperty("langchain4j.http.clientBuilderFactory", 
	// 	//     "dev.langchain4j.http.client.jdk.JdkHttpClientBuilderFactory");
	// }

	@Test
	void contextLoads() {
		 // 1. 初始化 Playwright 并启动浏览器
                try  {
                BrowserContext browser = getToken();

                // 2. 创建 BrowserUseTool
                BrowserUseTool browserTool = BrowserUseTool.from(
                        PlaywrightAoyaExecutionEngine.builder()
                                .browser(browser)
                                .build()
                );

                // 3. 创建 AI 模型（以 OpenAI 为例）
                ChatModel model = OpenAiChatModel.builder()
                                                .baseUrl(System.getenv("LLM_BASE_URL"))
                                                .apiKey(System.getenv("LLM_API_KEY"))
                                                .httpClientBuilder(new JdkHttpClientBuilder())
                                                .modelName(System.getenv().getOrDefault("LLM_MODEL", "GLM-5.2"))
                                                .build();

                // 4. 定义 Assistant 接口
                interface Assistant {
                        String chat(String message);
                }

                // 5. 构建 AI 服务，注册工具
                Assistant assistant = AiServices.builder(Assistant.class)
                        .chatModel(model)
                        .tools(browserTool)
                        .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                        .build();

                // 6. 用自然语言下达指令
                String question = "打开 'https://10.10.19.210/Assets/List' 页面，分析该页面的所有可交互元素，然后生成测试用例（不包含页面左侧的导航栏）**输出格式**：JSON 数组，每个用例包含 name（用例名称）、steps（操作步骤数组，每个步骤包含 action 和 selector/text）、expected（预期结果）。\r\n" + //
                                                "    \"\"\", elementsJson);，如果出现您的连接不是私密连接，选择高级-继续前往";
                System.out.println("AI 响应: "+assistant.chat(question));
                }catch(Exception e){
                        e.printStackTrace();
                }
        }

        @Test
        void getClassPath(){
                try (Playwright playwright = Playwright.create()) {
                        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                                .setHeadless(false)      // 显示浏览器窗口
                                .setChannel("chrome")    // 使用 Chrome
                                .setSlowMo(500);         // 放慢操作，便于观察
                        
                        Browser browser = playwright.chromium().launch(options);
                        //创建独立的上下文，并设置 setIgnoreHTTPSErrors(true)：忽略 HTTPS 证书错误
                        BrowserContext context = browser.newContext(
                                new Browser.NewContextOptions().setIgnoreHTTPSErrors(true)
                                );
                        Page page = context.newPage();
                        
                        while (true) {
                                page.navigate("https://10.10.19.210/Assets/List");
                                if (page.url().equals("https://10.10.19.210/Assets/List")){
                                        System.out.println("相等");
                                        break;
                                }  
                                System.out.println("不等");
                                page.locator("xpath=//*[@id=\"Login\"]/div[3]/div/div[2]/form/div[1]/div/div/span/span/input").fill(System.getenv().getOrDefault("AOYA_USER", "admin"));
                                page.locator("xpath=//*[@id=\"Login\"]/div[3]/div/div[2]/form/div[2]/div/div/span/span/input").fill(System.getenv("AOYA_PASSWORD"));
                                page.locator("xpath=//*[@id=\"Login\"]/div[3]/div/div[2]/form/div[3]/div/div/span/button").click();
                        }
                        page.waitForLoadState(LoadState.NETWORKIDLE);

                        String js = "() => {" +
                                "  const results = [];" +
                                "  const elements = document.querySelectorAll('button, input, select, textarea, a[href], [role=\"button\"], [role=\"link\"], [role=\"checkbox\"], [role=\"radio\"], [role=\"tab\"], [role=\"menuitem\"], [tabindex]:not([tabindex=\"-1\"])');" +
                                "  for (const el of elements) {" +
                                "    const rect = el.getBoundingClientRect();" +
                                "    if (rect.width === 0 || rect.height === 0) continue;" +
                                "    const data = {" +
                                "      tag: el.tagName.toLowerCase()," +
                                "      type: el.type || null," +
                                "      text: (el.innerText || '').trim().substring(0, 100) || null," +
                                "      id: el.id || null," +
                                "      className: el.className || null," +
                                "      name: el.name || null," +
                                "      value: el.value || null," +
                                "      placeholder: el.placeholder || null," +
                                "      ariaLabel: el.getAttribute('aria-label') || null," +
                                "      role: el.getAttribute('role') || null," +
                                "      tabindex: el.getAttribute('tabindex') || null," +
                                "      href: el.href || null," +
                                "      disabled: el.disabled || false," +
                                "      visible: true" +
                                "    };" +
                                "    results.push(data);" +
                                "  }" +
                                "  return results;" +
                                "}";

                        List<Map<String, Object>> result = (List<Map<String, Object>>) page.evaluate(js);
                        
                        System.out.println("xpath:"+result);
                } catch(Exception e ){
                        e.printStackTrace();
                }
                
        }

        private BrowserContext getToken() throws Exception{
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
                
                return context;
        }

}
