package com.uit.api.service.impl;

import org.springframework.stereotype.Service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.uit.api.entry.User;
import com.uit.api.service.TasksService;
import com.uit.api.utils.BrowserOperation;

@Service
public class TasksServiceImpl implements TasksService{


    @Override
    public void processTask(User user) {
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
}
