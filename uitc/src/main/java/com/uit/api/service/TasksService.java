package com.uit.api.service;

import com.uit.api.entry.Result;
import com.uit.api.entry.User;
import com.uit.api.vo.LoginStatus;

public interface TasksService {


    void processTask(User entity);

    void login(User user);

    LoginStatus loginStatus();
}
