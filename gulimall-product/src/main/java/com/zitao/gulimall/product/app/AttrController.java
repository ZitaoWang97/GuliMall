package com.zitao.gulimall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.zitao.gulimall.product.entity.ProductAttrValueEntity;
import com.zitao.gulimall.product.service.ProductAttrValueService;
import com.zitao.gulimall.product.vo.AttrRespVo;
import com.zitao.gulimall.product.vo.AttrVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.zitao.gulimall.product.service.AttrService;
import com.zitao.common.utils.PageUtils;
import com.zitao.common.utils.R;


/**
 * 商品属性
 *
 * @author Zitao Wang
 * @email zitao.wang@tum.de
 * @date 2022-04-26 21:20:37
 */
@RestController
@RequestMapping("product/attr")
public class AttrController {
    @Autowired
    private AttrService attrService;

    @Autowired
    ProductAttrValueService productAttrValueService;

    @GetMapping("/base/listforspu/{spuId}")
    public R listAttrsforSpu(@PathVariable("spuId") Long spuId) {
        List<ProductAttrValueEntity> productAttrValueEntities=productAttrValueService.listAttrsforSpu(spuId);
        return R.ok().put("data", productAttrValueEntities);
    }

    @PostMapping("/update/{spuId}")
    public R updateSpuAttrs(@PathVariable("spuId") Long spuId,
                            @RequestBody List<ProductAttrValueEntity> attrValueEntities) {
        productAttrValueService.updateSpuAttrs(spuId, attrValueEntities);
        return R.ok();
    }


    /**
     * 模糊查询三级分类对应的所有属性（规格参数 和 销售属性）
     * @param params
     * @param catelogId 三级分类id
     * @param type 规格参数："base" 销售属性："sale"
     * @return
     */
    @GetMapping("/{attrType}/list/{catelogId}")
    public R baseAttrList(@RequestParam Map<String, Object> params,
                          @PathVariable("catelogId") Long catelogId,
                          @PathVariable("attrType") String type) {
        PageUtils page = attrService.queryBaseAttrPage(params, catelogId, type);

        return R.ok().put("page", page);
    }


    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = attrService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     * 修改属性时，要回显三级分类的完整路径
     */
    @RequestMapping("/info/{attrId}")
    public R info(@PathVariable("attrId") Long attrId) {
        AttrRespVo respVo = attrService.getAttrInfo(attrId);

        return R.ok().put("attr", respVo);
    }

    /**
     * 保存
     * 保存属性attr的同时还要保存属性分组关联信息attr_attrgroup
     */
    @RequestMapping("/save")
    public R save(@RequestBody AttrVo attr) {
        attrService.saveAttr(attr);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody AttrVo attr) {
        attrService.updateAttr(attr);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] attrIds) {
        attrService.removeByIds(Arrays.asList(attrIds));

        return R.ok();
    }

}
