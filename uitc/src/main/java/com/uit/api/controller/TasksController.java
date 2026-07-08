package com.uit.api.controller;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uit.api.common.LoginType;
import com.uit.api.common.TaskPendingManager;
import com.uit.api.dto.TaskLoginDTO;
import com.uit.api.entry.LoginUser;
import com.uit.api.entry.Result;
import com.uit.api.entry.User;
import com.uit.api.service.TasksService;
import com.uit.api.utils.RedisKeyPrefix;
import com.uit.api.vo.LoginStatus;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/tasks")
public class TasksController {

    private final TasksService tasksService;
    
    private final TaskPendingManager taskPendingManager;

    private final RedisTemplate<String,Object> redisTemplate;

    public TasksController(TasksService tasksService,TaskPendingManager taskPendingManager,RedisTemplate<String,Object> redisTemplate){
        this.tasksService = tasksService;
        this.taskPendingManager = taskPendingManager;
        this.redisTemplate = redisTemplate;
    }

    @PostMapping
    public String postMethodName(@RequestBody LoginUser user) {
        //TODO: process POST request
        tasksService.processTask(user);
        return "";
    }

    @PostMapping("/url")
    public Result<Void> loginUrl(@RequestParam String url) {
        tasksService.analyzer(url);
        return Result.success();
    }

    @PostMapping("/submit")
    public Result<Void> loginSubmit(@RequestBody TaskLoginDTO taskLoginDTO) {
        CompletableFuture<LoginUser> future = taskPendingManager.get(taskLoginDTO.getTaskId());
        if (future == null){
            return Result.error(404, "未找到该任务");
        }
        if (taskLoginDTO.getLoginUser() == null) {
            return Result.error(404, "输入为空");
        }
        redisTemplate.opsForHash().put(RedisKeyPrefix.TASK_STATE + taskLoginDTO.getTaskId(), "status", 2);
        redisTemplate.opsForHash().put(RedisKeyPrefix.TASK_STATE + taskLoginDTO.getTaskId(), "LoginUser", taskLoginDTO.getLoginUser());
        future.complete(taskLoginDTO.getLoginUser());
        return Result.success();
    }
    
    @GetMapping("/status")
    public Result<LoginStatus> getLoginStatus() {
         LoginStatus loginStatus = tasksService.loginStatus();
        return Result.success(loginStatus);
    }
}
