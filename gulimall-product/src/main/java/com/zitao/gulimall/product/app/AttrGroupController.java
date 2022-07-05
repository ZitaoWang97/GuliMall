package com.zitao.gulimall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.zitao.gulimall.product.entity.AttrEntity;
import com.zitao.gulimall.product.service.AttrAttrgroupRelationService;
import com.zitao.gulimall.product.service.AttrService;
import com.zitao.gulimall.product.service.CategoryService;
import com.zitao.gulimall.product.vo.AttrGroupRelationVo;
import com.zitao.gulimall.product.vo.AttrGroupWithAttrsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.zitao.gulimall.product.entity.AttrGroupEntity;
import com.zitao.gulimall.product.service.AttrGroupService;
import com.zitao.common.utils.PageUtils;
import com.zitao.common.utils.R;


/**
 * 属性分组
 *
 * @author Zitao Wang
 * @email zitao.wang@tum.de
 * @date 2022-04-26 21:20:37
 */
@RestController
@RequestMapping("product/attrgroup")
public class AttrGroupController {
    @Autowired
    private AttrGroupService attrGroupService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    AttrService attrService;

    @Autowired
    AttrAttrgroupRelationService relationService;

    @GetMapping("/{catelogId}/withattr")
    public R getAttrGroupWithAttrByCatelogId(@PathVariable("catelogId") Long catId) {
        List<AttrGroupWithAttrsVo> groupWithAttrVos = attrGroupService.getAttrGroupWithAttrByCatelogId(catId);
        return R.ok().put("data", groupWithAttrVos);
    }

    @PostMapping("/attr/relation")
    public R saveBatch(@RequestBody List<AttrGroupRelationVo> vos) {
        relationService.saveBatch(vos);
        return R.ok();
    }

    @PostMapping("/attr/relation/delete")
    public R deleteRealtion(@RequestBody AttrGroupRelationVo[] vos) {
        attrService.deleteRelation(vos);
        return R.ok();
    }

    /**
     * 根据属性分组id找到分组内所有的属性
     * @param attrgroupId
     * @return
     */
    @GetMapping("/{attrgroupId}/attr/relation")
    public R attrRelation(@PathVariable("attrgroupId") Long attrgroupId) {
        List<AttrEntity> entities = attrService.getRelationAttr(attrgroupId);
        return R.ok().put("data", entities);
    }

    // http://localhost:88/api/product/attrgroup/2/noattr/relation?t=1656747489655&page=1&limit=10&key=
    @GetMapping("/{attrgroupId}/noattr/relation")
    public R attrNoRelation(@PathVariable("attrgroupId") Long attrgroupId,
                            @RequestParam Map<String, Object> params){
        PageUtils page = attrService.getNoRelationAttr(attrgroupId,params);
        return R.ok().put("page", page);
    }

    /**
     * 列表
     * 根据三级分类来查属性分组
     */
    @RequestMapping("/list/{catelogId}")
    public R list(@RequestParam Map<String, Object> params,
                  @PathVariable("catelogId") Long catelogId) {
//        PageUtils page = attrGroupService.queryPage(params);
        PageUtils page = attrGroupService.queryPage(params, catelogId);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrGroupId}")
    public R info(@PathVariable("attrGroupId") Long attrGroupId) {
        AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);
        // 查询属性分组所在三级分类的完整路径，用于数据回显
        Long catelogId = attrGroup.getCatelogId();
        Long[] path = categoryService.findCategoryPath(catelogId);
        attrGroup.setCatelogPath(path);

        return R.ok().put("attrGroup", attrGroup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.save(attrGroup);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.updateById(attrGroup);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] attrGroupIds) {
        attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return R.ok();
    }

}
