package com.uit.api.controller;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
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
import com.uit.api.utils.UserContext;
import com.uit.api.vo.LoginStatus;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@Slf4j
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

    @PostMapping(value = "/run", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> postMethodName(@RequestBody String msg) {
        Sinks.Many<Object> sink = Sinks.many().multicast().onBackpressureBuffer();
        tasksService.processTask(msg,sink);
        return sink.asFlux()
                .map(e -> ServerSentEvent.builder(e).event("progress").build())
                .doFinally(s -> log.info("SSE 流结束：{} " ,s));
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
