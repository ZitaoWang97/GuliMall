package com.zitao.gulimall.authserver.feign;


import com.zitao.common.utils.R;
import com.zitao.gulimall.authserver.feign.fallback.MemberFallbackService;
import com.zitao.gulimall.authserver.vo.SocialUser;
import com.zitao.gulimall.authserver.vo.UserLoginVo;
import com.zitao.gulimall.authserver.vo.UserRegisterVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient(value = "gulimall-member",fallback = MemberFallbackService.class)
public interface MemberFeignService {

    @RequestMapping("member/member/register")
    R register(@RequestBody UserRegisterVo registerVo);


    @RequestMapping("member/member/login")
     R login(@RequestBody UserLoginVo loginVo);

    @RequestMapping("member/member/oauth2/login")
    R login(@RequestBody SocialUser socialUser);
}
