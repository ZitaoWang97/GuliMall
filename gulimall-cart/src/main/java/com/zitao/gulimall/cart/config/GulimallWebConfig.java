package com.zitao.gulimall.cart.config;

import com.zitao.gulimall.cart.interceptor.CartInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class GulimallWebConfig implements WebMvcConfigurer {

    // 给拦截器的注册列表里添加自定义配置的拦截器
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 拦截当前购物车微服务里的所有请求
        registry.addInterceptor(new CartInterceptor()).addPathPatterns("/**");
    }
}
