package com.zitao.gulimall.product.dao;

import com.zitao.gulimall.product.entity.ProductAttrValueEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zitao.gulimall.product.vo.SpuItemAttrGroupVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * spu属性值
 * 
 * @author Zitao Wang
 * @email zitao.wang@tum.de
 * @date 2022-04-26 21:20:37
 */
@Mapper
public interface ProductAttrValueDao extends BaseMapper<ProductAttrValueEntity> {

    List<SpuItemAttrGroupVo> getProductGroupAttrsBySpuId(@Param("spuId") Long spuId, @Param("catalogId") Long catalogId);
}
