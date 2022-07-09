package com.zitao.gulimall.order.feign;


import com.zitao.common.to.SkuHasStockVo;
import com.zitao.common.utils.R;
import com.zitao.gulimall.order.vo.FareVo;
import com.zitao.gulimall.order.vo.WareSkuLockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@FeignClient("gulimall-ware")
public interface WareFeignService {

    @RequestMapping("ware/waresku/hasstock")
    List<SkuHasStockVo> getSkuHasStocks(@RequestBody List<Long> ids);

    @RequestMapping("ware/wareinfo/fare/{addrId}")
    FareVo getFare(@PathVariable("addrId") Long addrId);

    @RequestMapping("ware/waresku/lock/order")
    R orderLockStock(@RequestBody WareSkuLockVo itemVos);
}
