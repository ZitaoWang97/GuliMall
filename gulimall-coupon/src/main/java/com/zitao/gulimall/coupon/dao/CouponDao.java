package com.zitao.gulimall.coupon.dao;

import com.zitao.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author Zitao Wang
 * @email zitao.wang@tum.de
 * @date 2022-04-27 13:02:59
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
