package com.uit.api.common;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.uit.agentcore.agents.ExploerAnalyzer.Fields;
import com.uit.api.entry.LoginUser;

public enum LoginType {
    PASSWORD_ONLY {
        public boolean login(Page page,Fields fields,LoginUser user){
            page.locator(fields.username().css()).fill(user.getAccount());
            page.locator(fields.password().css()).fill(user.getPassword());
            page.locator(fields.submit().css()).click();
            Locator toast = page.locator(".ant-message");
            toast.waitFor(new Locator.WaitForOptions().setTimeout(2000));
            String message = toast.textContent();
            System.out.println(message);
            return !user.getUrl().equals(page.url());
        }
    },          //纯账号密码
    PASSWORD_CAPTCHA{
        public boolean login(Page page,Fields fields,LoginUser user){
            page.locator(fields.username().css()).fill(user.getAccount());
            page.locator(fields.password().css()).fill(user.getPassword());
            page.locator(fields.captchaInput().css()).fill(user.getCaptcha());
            page.locator(fields.submit().css()).click();
            Locator toast = page.locator(".ant-message");
            toast.waitFor(new Locator.WaitForOptions().setTimeout(2000));
            String message = toast.textContent();
            System.out.println(message);
            return !user.getUrl().equals(page.url());
        }
    },       //账号密码+验证码
    SMS{ 
        public boolean login(Page page,Fields fields,LoginUser user){
            
            page.locator(fields.username().xpath()).fill(fields.username().css());
            return true;
        }
    },                    //短信验证码
    QRCODE{                 //二维码
        public boolean login(Page page,Fields fields,LoginUser user){
            
            page.locator(fields.username().xpath()).fill(fields.username().css());
            return true;
        }
    },                  //滑块验证码
    SLIDER{                  //滑块验证码
        public boolean login(Page page,Fields fields,LoginUser user){
            
            page.locator(fields.username().xpath()).fill(fields.username().css());
            return true;
        }
    };

    public abstract boolean login(Page page,Fields fields,LoginUser user);
}
