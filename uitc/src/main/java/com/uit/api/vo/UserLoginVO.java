package com.uit.api.vo;

import lombok.Data;

@Data
public class UserLoginVO {
    private String userId;
    private String username;
    private String token;
}
