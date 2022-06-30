package com.zitao.gulimall.product.dao;

import com.zitao.gulimall.product.entity.SpuInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * spu信息
 * 
 * @author Zitao Wang
 * @email zitao.wang@tum.de
 * @date 2022-04-26 21:20:37
 */
@Mapper
public interface SpuInfoDao extends BaseMapper<SpuInfoEntity> {

    void upSpuStatus(@Param("spuId") Long spuId, @Param("code") int code);
}
