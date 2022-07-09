package com.zitao.gulimall.product.feign.fallback;


import com.zitao.common.exception.BizCodeEnume;
import com.zitao.common.utils.R;
import com.zitao.gulimall.product.feign.SeckillFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SeckillFallbackService implements SeckillFeignService {
    @Override
    public R getSeckillSkuInfo(Long skuId) {
        log.info("熔断方法调用...");
        return R.error(BizCodeEnume.READ_TIME_OUT_EXCEPTION.getCode(), BizCodeEnume.READ_TIME_OUT_EXCEPTION.getMsg());
    }
}
