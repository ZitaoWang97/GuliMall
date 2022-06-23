package com.zitao.gulimall.coupon.dao;

import com.zitao.gulimall.coupon.entity.MemberPriceEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品会员价格
 * 
 * @author Zitao Wang
 * @email zitao.wang@tum.de
 * @date 2022-04-27 13:02:59
 */
@Mapper
public interface MemberPriceDao extends BaseMapper<MemberPriceEntity> {
	
}
