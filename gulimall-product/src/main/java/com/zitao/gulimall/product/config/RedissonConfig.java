package com.zitao.gulimall.product.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    /**
     * 所有对Redission的使用都是通过RedissonClient
     * @return
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(){
        Config config = new Config();
        // 目前是单节点模式，之后可以使用集群模式
        config.useSingleServer().setAddress("redis://192.168.56.10:6379");
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }
}
