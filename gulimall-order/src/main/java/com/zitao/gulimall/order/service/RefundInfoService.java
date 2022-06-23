package com.zitao.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zitao.common.utils.PageUtils;
import com.zitao.gulimall.order.entity.RefundInfoEntity;

import java.util.Map;

/**
 * 退款信息
 *
 * @author Zitao Wang
 * @email zitao.wang@tum.de
 * @date 2022-04-27 14:40:37
 */
public interface RefundInfoService extends IService<RefundInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

