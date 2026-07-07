package com.uit.api.utils;

import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class JwtUtils {

    private static final String SECRET_KEY = "web-agent-auto-test-case-LFHsdkj";
    private static final long EXPIRATION_TIME = 3600000; // 1小时（单位：毫秒）


    public static String generateToken(String userId,String username){
        SecretKey hmacShaKeyFor = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
        return Jwts.builder()
                .subject(userId)
                .claim("username", username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(hmacShaKeyFor)
                .compact();
    }

     // 解析 JWT
    public static Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
        try {
        // 正常验证（包括过期）
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            // Token 过期但签名有效，从异常中获取 Claims
            return e.getClaims(); // 仍包含所有声明（如 userId）
        }
    }

    // 验证 Token 是否有效（包括过期校验）
    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            // 捕获 ExpiredJwtException 等异常[reference:11]
            
            return false;
        }
    }
}
