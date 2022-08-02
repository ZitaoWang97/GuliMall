package com.zitao.gulimall.authserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * 配置Spring Session
 */
@Configuration
public class GulimallSessionConfig {

    /**
     * 自定义序列化机制
     *
     * @return
     */
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer();
    }

    /**
     * 自定义cookie序列化器
     *
     * @return
     */
    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        // 自定义cookie的名字
        serializer.setCookieName("GULISESSIONID");
        // 设置cookie的作用域为父域
        serializer.setDomainName("gulimall.com");
        return serializer;
    }
}
