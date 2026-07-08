package com.uit.api.dto;

import com.uit.api.entry.LoginUser;

import lombok.Data;

@Data
public class TaskLoginDTO {

    private String taskId;
    
    private LoginUser loginUser;
}
