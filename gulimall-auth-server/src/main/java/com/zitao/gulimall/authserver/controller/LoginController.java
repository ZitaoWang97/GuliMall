package com.zitao.gulimall.authserver.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.zitao.common.constant.AuthServerConstant;
import com.zitao.common.exception.BizCodeEnume;
import com.zitao.common.utils.R;
import com.zitao.common.vo.MemberResponseVo;
import com.zitao.gulimall.authserver.feign.MemberFeignService;
import com.zitao.gulimall.authserver.feign.ThirdPartFeignService;
import com.zitao.gulimall.authserver.vo.UserLoginVo;
import com.zitao.gulimall.authserver.vo.UserRegisterVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController {
    @Autowired
    private ThirdPartFeignService thirdPartFeignService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MemberFeignService memberFeignService;

    @GetMapping("/login.html")
    public String loginPage(HttpSession session) {
        if (session.getAttribute(AuthServerConstant.LOGIN_USER) != null) {
            return "redirect:http://gulimall.com/";
        } else {
            return "login";
        }
    }

    /**
     * 用户登陆功能
     *
     * @param vo
     * @param attributes
     * @param session
     * @return
     */
    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes attributes, HttpSession session) {
        R r = memberFeignService.login(vo);
        if (r.getCode() == 0) {
            String jsonString = JSON.toJSONString(r.get("memberEntity"));
            MemberResponseVo memberResponseVo = JSON.parseObject(jsonString,
                    new TypeReference<MemberResponseVo>() {
            });
            // 向session中放入("loginUser", memberResponseVo)
            session.setAttribute(AuthServerConstant.LOGIN_USER, memberResponseVo);
            return "redirect:http://gulimall.com/";
        } else {
            String msg = (String) r.get("msg");
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", msg);
            attributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }


    /**
     * 发送验证码
     *
     * @param phone
     * @return
     */
    @GetMapping("/sms/sendCode")
    @ResponseBody
    public R sendCode(@RequestParam("phone") String phone) {
        //1. 接口防刷，在redis中缓存phone-code，防止同一个手机号码再60s内再次发送验证码
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        // 1.1 prePhone = "sms:code:18867136096"
        String prePhone = AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone;
        String v = ops.get(prePhone);
        if (!StringUtils.isEmpty(v)) {
            long pre = Long.parseLong(v.split("_")[1]);
            // 1.2 如果存储的时间小于60s，说明60s内发送过验证码
            if (System.currentTimeMillis() - pre < 60000) {
                return R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getCode(),
                        BizCodeEnume.SMS_CODE_EXCEPTION.getMsg());
            }
        }
        // 1.3 如果存在的话，删除之前的验证码
        redisTemplate.delete(prePhone);
        // 1.4 随机生成6位数字验证码
        String code = String.valueOf((int) ((Math.random() + 1) * 100000));
        // 2. 在redis中进行存储并设置过期时间10min
        ops.set(prePhone, code + "_" + System.currentTimeMillis(), 10, TimeUnit.MINUTES);
        // 3. 调用第三方服务 给手机发送验证码
        thirdPartFeignService.sendCode(phone, code);
        return R.ok();
    }


    /**
     * 注册新用户
     *
     * @param registerVo
     * @param result
     * @param attributes 重定向携带数据
     * @return
     */
    @PostMapping("/register")
    public String register(@Valid UserRegisterVo registerVo, BindingResult result,
                           RedirectAttributes attributes) {
        // 判断JSR303校验是否通过
        Map<String, String> errors = new HashMap<>();
        if (result.hasErrors()) {
            // 1. 如果校验不通过，则封装校验结果
            result.getFieldErrors().forEach(item -> {
                errors.put(item.getField(), item.getDefaultMessage());
                // 1.1 将错误信息封装到session中，重定向到新页面后从session中取出数据后，就会自动将数据删掉 TODO 分布式下session问题
                attributes.addFlashAttribute("errors", errors);
            });
            //1.2 重定向到注册页
            return "redirect:http://auth.gulimall.com/reg.html";
        } else {
            // 2. 若JSR303校验通过
            String code = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + registerVo.getPhone());
            // 2.1 如果对应手机的验证码不为空且与redis中存储的相等 -> 验证码正确
            if (!StringUtils.isEmpty(code) && registerVo.getCode().equals(code.split("_")[0])) {
                // 2.1.1 使得验证后的验证码失效 即在redis中删除 令牌机制
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + registerVo.getPhone());
                // 2.1.2 远程调用会员服务注册
                R r = memberFeignService.register(registerVo);
                if (r.getCode() == 0) {
                    //调用成功，重定向登录页
                    return "redirect:http://auth.gulimall.com/login.html";
                } else {
                    // 2.1.3 调用失败，返回注册页并显示错误信息
                    String msg = (String) r.get("msg");
                    errors.put("msg", msg);
                    attributes.addFlashAttribute("errors", errors);
                    return "redirect:http://auth.gulimall.com/reg.html";
                }
            } else {
                // 2.2 验证码错误
                errors.put("code", "验证码错误");
                attributes.addFlashAttribute("errors", errors);
                return "redirect:http://auth.gulimall.com/reg.html";
            }
        }
    }
}
