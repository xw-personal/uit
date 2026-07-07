package com.uit.api.interceptor;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.api.entry.Result;
import com.uit.api.entry.UserInfo;
import com.uit.api.utils.JwtUtils;
import com.uit.api.utils.UserContext;
import com.uit.api.websocket.WebsocketService;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@Component
public class AuthInterceptor implements HandlerInterceptor{

    private final ObjectMapper objectMapper;
    private final WebsocketService websocketService;

    public AuthInterceptor(ObjectMapper objectMapper,WebsocketService websocketService){
        this.objectMapper = objectMapper;
        this.websocketService = websocketService;
    }

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        if (token.isEmpty()){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            String resultJson = objectMapper.writeValueAsString(Result.error(401, "请先登录"));
            response.getWriter().write(resultJson);
            return false;
        }
        Claims claims = JwtUtils.parseToken(token);
        if (!JwtUtils.validateToken(token)){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            String resultJson = objectMapper.writeValueAsString(Result.error(401, "登录到期，请重新登录"));
            response.getWriter().write(resultJson);
            websocketService.kickUser(claims.getSubject(), "token到期");
            return false;
        }
        UserInfo userInfo = new UserInfo();
        userInfo.setId(claims.getSubject());
        userInfo.setUsername(claims.get("username", String.class));
        UserContext.setUser(userInfo);
        return true;
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 无论成功还是异常，请求结束后必须清理
        UserContext.clear();
        // 如果是用 SecurityContextHolder，也要清理
        SecurityContextHolder.clearContext();
    }
}
