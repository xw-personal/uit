package com.uit.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.web.bind.annotation.RestController;

import com.uit.api.entry.Result;
import com.uit.api.utils.UserContext;
import com.uit.api.websocket.WebsocketService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.GetMapping;


@Slf4j
@RestController
public class TestWebSocketController {


    @Autowired
    private WebsocketService websocketService;

    @Autowired
    private SimpUserRegistry simpUserRegistry;

    @GetMapping("/socket/test")
    public Result socketTest() {
        String userId = UserContext.getUser().getId();
        log.info("[PUSH] 推送目标 userId={}", userId);
        log.info("[PUSH] SimpUserRegistry.getUser(userId)={}", simpUserRegistry.getUser(userId));
        log.info("[PUSH] 当前已注册用户数={}", simpUserRegistry.getUsers().size());
        simpUserRegistry.getUsers().forEach(u ->
                log.info("  - 已注册用户: {} sessions={}", u.getName(), u.getSessions().size()));
        websocketService.pushAnalysisResult(userId, "测试推送消息");
        return Result.success();
    }

}
