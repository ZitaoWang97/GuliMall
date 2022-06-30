package com.zitao.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zitao.common.utils.PageUtils;
import com.zitao.gulimall.product.entity.CategoryEntity;
import com.zitao.gulimall.product.vo.Catalog2Vo;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author Zitao Wang
 * @email zitao.wang@tum.de
 * @date 2022-04-26 21:20:37
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<CategoryEntity> listWithTree();

    void removeMenuByIds(List<Long> asList);

    Long[] findCategoryPath(Long catelogId);

    void updateCascade(CategoryEntity category);

    Map<String, List<Catalog2Vo>> getCatalogJsonDbWithSpringCache();

    List<CategoryEntity> getLevel1Categories();
}

