package com.uit.agentcore.tools;


import com.microsoft.playwright.Page;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

public class PageEvaluateTool {
    private final Page page;

    public PageEvaluateTool(Page page) {
        this.page = page;
    }
    @Tool(name = "browser_evaluateJs",value = "在当前页面执行一段 JavaScript 并返回结果(JSON)。可用于 DOM 采集、读取元素属性/" +
            "坐标、调用 elementFromPoint 等。script 是函数体或表达式，如 " +
            "() => document.querySelectorAll('button').length")
    public Object evaluateJs(@P("要执行的 JS，可以是箭头函数 () => {...} 或表达式") String script){
        Object result = this.page.evaluate(script);
        return result == null ? "null" : result;
    }

    // @Tool("打开指定页面，但是跳转到登录页或者弹出登录框时，通过xpath定位元素执行登录操作。")
    // public void login(@P("用于定位登录元素xpath的Map，key为'username'、'password'、'loginButton'") Map<String, String> xpathMap) {
    //     String userXpath = xpathMap.get("username");
    //     String passXpath = xpathMap.get("password");
    //     String btnXpath = xpathMap.get("loginButton");
        
    //     // 登录逻辑
    // }

    // // 人机回环方法：等待用户输入凭据
    // @HumanInTheLoop(
    //     description = "等待用户提供用户名和密码",
    //     outputKey = "loginCredentials"
    // )
    // static Map<String, String> askCredentials(@P("prompt") String prompt) {
    //     // 这里可以是从控制台读取，或者返回 PendingResponse 等待外部 API
    //     System.out.println(prompt);
    //     Scanner scanner = new Scanner(System.in);
    //     System.out.print("用户名: ");
    //     String username = scanner.nextLine();
    //     System.out.print("密码: ");
    //     String password = scanner.nextLine();
    //     return Map.of("username", username, "password", password);
    // }
}
