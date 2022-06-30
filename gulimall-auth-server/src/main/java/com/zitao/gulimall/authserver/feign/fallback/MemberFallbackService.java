package com.zitao.gulimall.authserver.feign.fallback;


import com.zitao.common.exception.BizCodeEnume;
import com.zitao.common.utils.R;
import com.zitao.gulimall.authserver.feign.MemberFeignService;
import com.zitao.gulimall.authserver.vo.SocialUser;
import com.zitao.gulimall.authserver.vo.UserLoginVo;
import com.zitao.gulimall.authserver.vo.UserRegisterVo;
import org.springframework.stereotype.Service;

@Service
public class MemberFallbackService implements MemberFeignService {
    @Override
    public R register(UserRegisterVo registerVo) {
        return R.error(BizCodeEnume.READ_TIME_OUT_EXCEPTION.getCode(), BizCodeEnume.READ_TIME_OUT_EXCEPTION.getMsg());
    }

    @Override
    public R login(UserLoginVo loginVo) {
        return R.error(BizCodeEnume.READ_TIME_OUT_EXCEPTION.getCode(), BizCodeEnume.READ_TIME_OUT_EXCEPTION.getMsg());
    }

    @Override
    public R login(SocialUser socialUser) {
        return R.error(BizCodeEnume.READ_TIME_OUT_EXCEPTION.getCode(), BizCodeEnume.READ_TIME_OUT_EXCEPTION.getMsg());
    }
}
