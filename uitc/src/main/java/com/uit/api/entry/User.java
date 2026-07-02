package com.uit.api.entry;

import lombok.Data;

@Data
public class User {
    private String url;
    private String account;
    private String password;
    private String captcha;
}
