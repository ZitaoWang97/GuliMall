package com.zitao.gulimall.product.service.impl;

import com.zitao.gulimall.product.entity.AttrEntity;
import com.zitao.gulimall.product.service.AttrService;
import com.zitao.gulimall.product.vo.AttrGroupWithAttrsVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zitao.common.utils.PageUtils;
import com.zitao.common.utils.Query;

import com.zitao.gulimall.product.dao.AttrGroupDao;
import com.zitao.gulimall.product.entity.AttrGroupEntity;
import com.zitao.gulimall.product.service.AttrGroupService;
import org.springframework.util.StringUtils;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    AttrService attrService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                // 将前端获取来的参数params封装为IPage对象，包含当前页码、每页记录数、排序等信息
                new Query<AttrGroupEntity>().getPage(params),
                // 封装一个查询条件
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }


    /**
     * 多字段模糊匹配 查询对应的属性分组
     *
     * @param params
     * @param catelogId
     * @return
     */
    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {
        // 获取检索的参数 前端中为“key”参数
        String key = (String) params.get("key");
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<AttrGroupEntity>();
        if (!StringUtils.isEmpty(key)) {
            wrapper.and((obj) -> {
                obj.eq("attr_group_id", key).or().like("attr_group_name", key);
            });
        }
        if (catelogId == 0) {
            // catelogId为0则查所有属性
            IPage<AttrGroupEntity> page = this.page(
                    new Query<AttrGroupEntity>().getPage(params),
                    wrapper
            );
            return new PageUtils(page);
        } else {
            // 按照三级分类查
            wrapper.eq("catelog_id", catelogId);
            IPage<AttrGroupEntity> page = this.page(
                    new Query<AttrGroupEntity>().getPage(params),
                    wrapper
            );
            return new PageUtils(page);
        }
    }

    /**
     * 根据分类id查出所有的分组以及这些组里面的属性
     *
     * @param catId
     * @return
     */
    @Override
    public List<AttrGroupWithAttrsVo> getAttrGroupWithAttrByCatelogId(Long catId) {
        // 1. 查询所有的属性分组
        List<AttrGroupEntity> attrGroupEntities = baseMapper.selectList(
                new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catId));
        // 2. 查询所有分组内的属性
        List<AttrGroupWithAttrsVo> collect = attrGroupEntities.stream().map(group -> {
                    AttrGroupWithAttrsVo vo = new AttrGroupWithAttrsVo();
                    BeanUtils.copyProperties(group, vo);
                    List<AttrEntity> relationAttr = attrService.getRelationAttr(group.getAttrGroupId());
                    vo.setAttrs(relationAttr);
                    return vo;
                }
        ).collect(Collectors.toList());
        return collect;
    }

}