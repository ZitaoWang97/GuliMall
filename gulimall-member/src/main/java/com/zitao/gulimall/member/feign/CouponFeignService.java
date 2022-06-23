package com.zitao.gulimall.member.feign;

import com.zitao.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

// 编写一个接口，告诉SpringCloud这个接口需要调用远程服务，并且指定想要调用的远程服务的名称
@FeignClient("gulimall-coupon")
public interface CouponFeignService {
    // 调用远程微服务中对应请求的对应方法
    @RequestMapping("/coupon/coupon/member/list")
    public R membercoupons();
}
