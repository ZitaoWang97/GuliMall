package com.zitao.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zitao.common.to.mq.SeckillOrderTo;
import com.zitao.common.utils.PageUtils;
import com.zitao.gulimall.order.entity.OrderEntity;
import com.zitao.gulimall.order.vo.*;

import java.util.Map;

/**
 * 订单
 *
 * @author Zitao Wang
 * @email zitao.wang@tum.de
 * @date 2022-04-27 14:40:37
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void closeOrder(OrderEntity orderEntity);

    void handlerPayResult(PayAsyncVo payAsyncVo);

    void createSeckillOrder(SeckillOrderTo orderTo);

    OrderConfirmVo confirmOrder();

    SubmitOrderResponseVo submitOrder(OrderSubmitVo submitVo);

    PageUtils getMemberOrderPage(Map<String, Object> params);

    PayVo getOrderPay(String orderSn);
}

