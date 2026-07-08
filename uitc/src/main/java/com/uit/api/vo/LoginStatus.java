package com.uit.api.vo;

import lombok.Data;

@Data
public class LoginStatus {
    private Byte status;
    private String url;
    private String expireTime;
}
