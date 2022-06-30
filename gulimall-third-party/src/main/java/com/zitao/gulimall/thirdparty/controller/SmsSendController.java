package com.zitao.gulimall.thirdparty.controller;


import com.zitao.common.utils.R;
import com.zitao.gulimall.thirdparty.component.SmsComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping(value = "/sms")
public class SmsSendController {

    @Autowired
    private SmsComponent smsComponent;

    /**
     * 提供给别的服务进行调用
     * @param phone
     * @param code
     * @return
     */
    @ResponseBody
    @GetMapping(value = "/sendCode")
    public R sendCode(@RequestParam("phone") String phone, @RequestParam("code") String code) {

        //发送验证码
        smsComponent.sendCode(phone,code);
        System.out.println(phone+code);
        return R.ok();
    }

}
