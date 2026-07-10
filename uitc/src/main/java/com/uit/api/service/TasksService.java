package com.uit.api.service;

import com.uit.api.entry.LoginUser;
import com.uit.api.entry.Result;
import com.uit.api.entry.User;
import com.uit.api.vo.LoginStatus;

import reactor.core.publisher.Sinks;

public interface TasksService {


    void processTask(String msg, Sinks.Many<Object> sink);

    void analyzer(String url);


    LoginStatus loginStatus();
}
