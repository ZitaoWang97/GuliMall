package com.zitao.gulimall.order.dao;

import com.zitao.gulimall.order.entity.OrderItemEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单项信息
 * 
 * @author Zitao Wang
 * @email zitao.wang@tum.de
 * @date 2022-04-27 14:40:37
 */
@Mapper
public interface OrderItemDao extends BaseMapper<OrderItemEntity> {
	
}
