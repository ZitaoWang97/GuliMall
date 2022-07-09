package com.zitao.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zitao.common.to.SkuHasStockVo;
import com.zitao.common.to.mq.OrderTo;
import com.zitao.common.to.mq.StockLockedTo;
import com.zitao.common.utils.PageUtils;
import com.zitao.gulimall.ware.entity.WareSkuEntity;
import com.zitao.gulimall.ware.vo.WareSkuLockVo;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author Zitao Wang
 * @email zitao.wang@tum.de
 * @date 2022-04-27 14:42:12
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockVo> getSkuHasStocks(List<Long> ids);

    Boolean orderLockStock(WareSkuLockVo lockVo);

    void unlock(StockLockedTo stockLockedTo);

    void unlock(OrderTo orderTo);
}

