package com.zitao.gulimall.authserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AuthWebConfig implements WebMvcConfigurer {

    /**
     * 配置视图映射
     *
     * @param registry
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login.html").setViewName("login");
        /**
         *     @GetMapping("/reg.html")
         *     public String regPage() {
         *         return "reg";
         *     }
         */
        registry.addViewController("/reg.html").setViewName("reg");
    }
}
