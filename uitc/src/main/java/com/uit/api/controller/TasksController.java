package com.uit.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uit.api.entry.User;
import com.uit.api.service.TasksService;

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
    
}
