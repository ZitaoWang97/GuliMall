package com.zitao.gulimall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@EnableRedisHttpSession
@EnableCaching
@EnableFeignClients(basePackages = "com.zitao.gulimall.product.feign") // 开启远程调用功能
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.zitao.gulimall.product.dao")
// 在主程序应用类上使用`@MapperScan("com.zitao.boot.mapper")` 注解简化，Dao接口就可以不用标注`@Mapper`注解
public class GulimallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallProductApplication.class, args);
    }

}
