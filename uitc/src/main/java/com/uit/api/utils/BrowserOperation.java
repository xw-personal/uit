package com.uit.api.utils;

import java.nio.file.Path;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.uit.api.entry.User;

public class BrowserOperation {


    public static void login(User user, BrowserContext context) {
        // Implement the login logic here using the provided user credentials
        // For example, you can use Playwright to navigate to the login page, fill in the credentials, and submit the form
        // After successful login, return the updated BrowserContext
        Page page = context.newPage();
                page.navigate("https://10.10.19.210/login");
                System.out.println("开始登录");
                page.locator("xpath=//*[@id=\"Login\"]/div[3]/div/div[2]/form/div[1]/div/div/span/span/input").fill(user.getAccount());
                page.locator("xpath=//*[@id=\"Login\"]/div[3]/div/div[2]/form/div[2]/div/div/span/span/input").fill(user.getPassword());
                page.locator("xpath=//*[@id=\"Login\"]/div[3]/div/div[2]/form/div[3]/div/div/span/button").click();
                if (isXPathExists(page, "//*[@id=\"Login\"]/div[3]/div/div[2]/form/div[3]/div[1]/div/div/div/span/span/input")) {
                    System.out.println("登录失败，请先关闭登录验证码");
                }
                context.storageState(new BrowserContext.StorageStateOptions().setPath(Path.of("auth.json")));
    }

    private static Boolean isXPathExists(Page page, String locator){
        return page.locator(locator).count() > 0;
    }
}
