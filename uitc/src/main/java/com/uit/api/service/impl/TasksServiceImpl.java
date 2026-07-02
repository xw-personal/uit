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

import org.json.JSONObject;
import org.springframework.stereotype.Service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import com.uit.api.entry.Result;
import com.uit.api.entry.User;
import com.uit.api.handler.BusinessException;
import com.uit.api.service.TasksService;
import com.uit.api.utils.BrowserOperation;
import com.uit.api.vo.LoginStatus;

import dev.langchain4j.internal.Json;

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

    @Override
    public void login(User user) {
        Playwright playwright = Playwright.create();
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(true)      // 显示浏览器窗口
                .setChannel("chrome")    // 使用 Chrome
                .setSlowMo(100);
        Browser browser = playwright.chromium().launch(options);
        //创建独立的上下文，并设置 setIgnoreHTTPSErrors(true)：忽略 HTTPS 证书错误
        BrowserContext context = browser.newContext(
                new Browser.NewContextOptions().setIgnoreHTTPSErrors(true)
                );
        Page page = context.newPage();
        page.navigate(user.getUrl());
        page.getByRole(AriaRole.TEXTBOX,new Page.GetByRoleOptions().setName("用户账号")).fill(user.getAccount());
        page.getByRole(AriaRole.TEXTBOX,new Page.GetByRoleOptions().setName("密码")).fill(user.getPassword());
        page.getByRole(AriaRole.BUTTON,new Page.GetByRoleOptions().setName("登录")).click();
        if (page.url().equals(user.getUrl())){
            System.out.println("登录失败，请检测账号密码是否正确，或关闭登录验证码");
        }
        context.storageState(new BrowserContext.StorageStateOptions().setPath(Path.of("auth.json")));
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
}
