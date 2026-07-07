package com.uit.api.websocket;

import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;


import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WebsocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public WebsocketService(SimpMessagingTemplate simpMessagingTemplate) {
        this.messagingTemplate = simpMessagingTemplate;
    }

    public <T> void pushAnalysisResult(String userId, T result) {
        // 这里可以使用 WebSocket 或其他方式将分析结果推送到前端
        // 例如，使用 Spring 的 SimpMessagingTemplate 发送消息到指定的 WebSocket 主题
        // simpMessagingTemplate.convertAndSend("/topic/analysisResult/" + taskId, result);
        
        messagingTemplate.convertAndSendToUser(userId, "/queue/analysis", result);
    }


    public void kickUser(String userId,String msg) {
        // 发送一个自定义的 KICK 事件到该用户的专属队列
        // 前端订阅的是 /user/queue/kick
        messagingTemplate.convertAndSendToUser(
            userId, 
            "/queue/kick", 
            Map.of("code", 1001, "message", msg)
        );
        log.info("已发送踢出指令给用户: " + userId);
    }
    
}
