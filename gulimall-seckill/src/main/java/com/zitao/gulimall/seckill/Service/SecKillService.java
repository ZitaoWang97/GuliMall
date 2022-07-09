package com.zitao.gulimall.seckill.Service;


import com.zitao.gulimall.seckill.to.SeckillSkuRedisTo;

import java.util.List;

public interface SecKillService {
    void uploadSeckillSkuLatest3Days();

    List<SeckillSkuRedisTo> getCurrentSeckillSkus();

    SeckillSkuRedisTo getSeckillSkuInfo(Long skuId);

    String kill(String killId, String key, Integer num) throws InterruptedException;
}
