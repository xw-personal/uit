package com.uit.api.entry;

import lombok.Data;

@Data
public class LoginUser {
    private String url;
    private String account;
    private String password;
    private String captcha;
}
