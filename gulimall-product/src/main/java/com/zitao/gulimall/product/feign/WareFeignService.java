package com.zitao.gulimall.product.feign;


import com.zitao.common.to.SkuHasStockVo;
import com.zitao.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@FeignClient("gulimall-ware")
public interface WareFeignService {

    @RequestMapping("/ware/waresku/hasstock")
    List<SkuHasStockVo> getSkuHasStocks(@RequestBody List<Long> ids);
}
