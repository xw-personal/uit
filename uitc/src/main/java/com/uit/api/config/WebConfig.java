package com.uit.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.uit.api.interceptor.AuthInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer{

    private final AuthInterceptor authInterceptor;


    @Value("${yolo.service.base-url}")
    private String yoloBaseUrl;

    public WebConfig(AuthInterceptor authInterceptor){
        this.authInterceptor = authInterceptor;
    }

    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/user/login","/user/register");
    }   

    @Bean
    public RestClient createClient(){
        return RestClient.builder()
                .baseUrl(yoloBaseUrl)
                .defaultHeader("Authorization", "Bearer your-token") // 可放其他通用头
                .build();
    }

}
