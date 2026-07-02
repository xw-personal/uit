package com.uit.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uit.api.entry.Result;
import com.uit.api.entry.User;
import com.uit.api.service.TasksService;
import com.uit.api.vo.LoginStatus;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/tasks")
public class TasksController {
    @Autowired
    private TasksService tasksService;

    @PostMapping
    public String postMethodName(@RequestBody User user) {
        //TODO: process POST request
        tasksService.processTask(user);
        return "";
    }

    @PostMapping("/login")
    public Result<Void> login(@RequestBody User user) {
        tasksService.login(user);
        return Result.success();
    }
    
    @GetMapping("/status")
    public Result<LoginStatus> getLoginStatus() {
        LoginStatus loginStatus = tasksService.loginStatus();
        return Result.success(loginStatus);
    }
}
