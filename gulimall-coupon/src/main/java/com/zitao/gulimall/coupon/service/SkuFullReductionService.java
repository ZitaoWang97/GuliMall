package com.zitao.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zitao.common.to.SkuReductionTo;
import com.zitao.common.utils.PageUtils;
import com.zitao.gulimall.coupon.entity.SkuFullReductionEntity;

import java.util.Map;

/**
 * 商品满减信息
 *
 * @author Zitao Wang
 * @email zitao.wang@tum.de
 * @date 2022-04-27 13:02:59
 */
public interface SkuFullReductionService extends IService<SkuFullReductionEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSkuReductionTo(SkuReductionTo skuReductionTo);
}

