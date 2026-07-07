package com.uit.api.config;

import java.security.Principal;
import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import com.uit.api.utils.JwtUtils;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // token 走 URL query param（SockJS 不支持自定义握手头）：
        //   1) TokenHandshakeInterceptor 解析 JWT → 把 userId 放入 attributes
        //   2) UserHandshakeHandler.determineUser() 把 attributes.userId 提升为 WebSocket 会话的 Principal
        // 这样 Principal 在 STOMP CONNECT 之前就落在会话上，SimpUserRegistry 一定会注册。
        registry.addEndpoint("/ws-uit")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new TokenHandshakeInterceptor())
                .setHandshakeHandler(new UserHandshakeHandler())
                .withSockJS();
    }

    /** 从 URL ?token=xxx 解析 JWT，把 userId 放入 session attributes。 */
    static class TokenHandshakeInterceptor implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) {
            String token = extractTokenFromQuery(request);
            if (token != null) {
                try {
                    Claims claims = JwtUtils.parseToken(token);
                    String userId = claims.getSubject();
                    attributes.put("userId", userId);
                    log.info("[HANDSHAKE] userId={} 已放入 session attributes", userId);
                } catch (Exception e) {
                    log.warn("[HANDSHAKE] token 解析失败: {}", e.getMessage());
                    return false;
                }
            }
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) {
        }

        private String extractTokenFromQuery(ServerHttpRequest request) {
            String query = request.getURI().getQuery();
            if (query == null) return null;
            for (String kv : query.split("&")) {
                int i = kv.indexOf('=');
                if (i > 0 && "token".equals(kv.substring(0, i))) {
                    return kv.substring(i + 1);
                }
            }
            return null;
        }
    }

    /** 把握手 attributes 里的 userId 提升为 WebSocket 会话的 Principal。 */
    static class UserHandshakeHandler extends DefaultHandshakeHandler {
        @Override
        protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                          Map<String, Object> attributes) {
            String userId = (String) attributes.get("userId");
            if (userId == null) return super.determineUser(request, wsHandler, attributes);
            log.info("[HANDSHAKE] 设置会话 Principal, userId={}", userId);
            return () -> userId;
        }
    }

    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Principal 已由 HandshakeHandler 在握手阶段设到会话上，这里不再需要 setUser。
        // 保留 CONNECT 日志便于排查。
        registration.interceptors(new ChannelInterceptor() {
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    Principal p = accessor.getUser();
                    log.info("[WS-CONNECT] CONNECT 收到, session Principal={}",
                            p == null ? "(null)" : p.getName());
                }
                return message;
            }
        });
    }

}
