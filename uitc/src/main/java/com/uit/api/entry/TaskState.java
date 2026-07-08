package com.uit.api.entry;

import com.uit.api.common.LoginType;

import lombok.Data;

@Data
public class TaskState {
    //状态：1 待处理，2 处理中，0 已完成
    private Byte status;

    private LoginUser inputUser;

    private LoginType loginType;
}
