package com.zitao.gulimall.product.service.impl;

import com.zitao.gulimall.product.vo.SpuItemAttrGroupVo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zitao.common.utils.PageUtils;
import com.zitao.common.utils.Query;

import com.zitao.gulimall.product.dao.ProductAttrValueDao;
import com.zitao.gulimall.product.entity.ProductAttrValueEntity;
import com.zitao.gulimall.product.service.ProductAttrValueService;
import org.springframework.transaction.annotation.Transactional;


@Service("productAttrValueService")
public class ProductAttrValueServiceImpl extends ServiceImpl<ProductAttrValueDao, ProductAttrValueEntity> implements ProductAttrValueService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<ProductAttrValueEntity> page = this.page(
                new Query<ProductAttrValueEntity>().getPage(params),
                new QueryWrapper<ProductAttrValueEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<ProductAttrValueEntity> listAttrsforSpu(Long spuId) {
        List<ProductAttrValueEntity> entities = this.baseMapper.selectList(new QueryWrapper<ProductAttrValueEntity>().eq("spu_id", spuId));
        return entities;
    }

    @Transactional
    @Override
    public void updateSpuAttrs(Long spuId, List<ProductAttrValueEntity> attrValueEntities) {
        // 先把之前的属性删了
        this.baseMapper.delete(new QueryWrapper<ProductAttrValueEntity>().eq("spu_id", spuId));
        // 后续就是新增操作，而不用去纠结是insert还是update
        List<ProductAttrValueEntity> collect = attrValueEntities.stream().map(item -> {
            item.setSpuId(spuId);
            return item;
        }).collect(Collectors.toList());
        this.saveBatch(collect);
    }

    @Override
    public List<SpuItemAttrGroupVo> getProductGroupAttrsBySpuId(Long spuId, Long catalogId) {
        return baseMapper.getProductGroupAttrsBySpuId(spuId, catalogId);
    }

}