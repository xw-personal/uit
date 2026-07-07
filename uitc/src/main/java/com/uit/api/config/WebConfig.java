package com.uit.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.uit.api.interceptor.AuthInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer{

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor){
        this.authInterceptor = authInterceptor;
    }

    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/user/login","/user/register");
    }   
}
