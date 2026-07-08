package com.uit.api.common;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.uit.api.entry.LoginUser;

@Component
public class TaskPendingManager {
    private final Map<String,CompletableFuture<LoginUser>> pendingMap = new ConcurrentHashMap<>();

    // 存储 future
    public void put(String taskId, CompletableFuture<LoginUser> future) {
        pendingMap.put(taskId, future);
    }

    // 获取并移除 future（通常唤醒后即移除）
    public CompletableFuture<LoginUser> remove(String taskId) {
        return pendingMap.remove(taskId);
    }

    // 仅查询（用于调试或状态判断）
    public CompletableFuture<LoginUser> get(String taskId) {
        return pendingMap.get(taskId);
    }
}
