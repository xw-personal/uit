package com.uit.api.common;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.uit.agentcore.agents.ExploerAnalyzer.Fields;
import com.uit.api.entry.LoginUser;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum LoginType {
    PASSWORD_ONLY {
        public String login(Page page,Fields fields,LoginUser user){
            
            page.locator(fields.username().css()).fill(user.getAccount());
            page.locator(fields.password().css()).fill(user.getPassword());
            page.locator(fields.submit().css()).click();
            Locator toast = page.locator(".ant-message");
            toast.waitFor(new Locator.WaitForOptions().setTimeout(2000));
            String message = toast.textContent();
            log.info("PASSWORD_ONLY登录:"+message);
            return message;
        }
    },          //纯账号密码
    PASSWORD_CAPTCHA{
        
        public String login(Page page,Fields fields,LoginUser user){     
   
            page.locator(fields.username().css()).fill(user.getAccount());
            page.locator(fields.password().css()).fill(user.getPassword());
            page.locator(fields.captchaInput().css()).fill(user.getCaptcha());
            page.locator(fields.submit().css()).click();
            Locator toast = page.locator(".ant-message");
            toast.waitFor(new Locator.WaitForOptions().setTimeout(2000));
            String message = toast.textContent();
            log.info("PASSWORD_CAPTCHA登录:"+message);
            return message;
        }
    },       //账号密码+验证码
    SMS{ 
        public String login(Page page,Fields fields,LoginUser user){
            
            page.locator(fields.username().xpath()).fill(fields.username().css());
            return "";
        }
    },                    //短信验证码
    QRCODE{                 //二维码
        public String login(Page page,Fields fields,LoginUser user){
            
            page.locator(fields.username().xpath()).fill(fields.username().css());
            return "";
        }
    },                  //滑块验证码
    SLIDER{                  //滑块验证码
        public String login(Page page,Fields fields,LoginUser user){
            
            page.locator(fields.username().xpath()).fill(fields.username().css());
            return "";
        }
    };

    public abstract String login(Page page,Fields fields,LoginUser user);
}
