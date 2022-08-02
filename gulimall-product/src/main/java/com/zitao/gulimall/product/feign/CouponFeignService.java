package com.zitao.gulimall.product.feign;


import com.zitao.common.to.SkuReductionTo;
import com.zitao.common.to.SpuBoundTo;
import com.zitao.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("gulimall-coupon") // 声明调用哪个微服务
public interface CouponFeignService {

    /**
     * spring cloud open-feign步骤：
     * 1. 调用该方法时，@RequestBody将对象转为json
     * 2. 找到gulimall-coupon/coupon/spubounds/save，将json数据放在请求体的位置，发送请求
     * 3. coupon服务收到请求，请求体带着json数据，@RequestBody将请求体的json转为对象
     * @param spuBoundTo
     * @return
     */
    @PostMapping("/coupon/spubounds/save")
    R saveSpuBounds(@RequestBody SpuBoundTo spuBoundTo);

    @PostMapping("/coupon/skufullreduction/saveinfo")
    R saveSkuReductionTo(@RequestBody SkuReductionTo skuReductionTo);
}
