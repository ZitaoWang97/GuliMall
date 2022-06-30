package com.zitao.gulimall.product.app;

import java.util.Arrays;
import java.util.Map;

import com.zitao.gulimall.product.vo.SpuSaveVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.zitao.gulimall.product.entity.SpuInfoEntity;
import com.zitao.gulimall.product.service.SpuInfoService;
import com.zitao.common.utils.PageUtils;
import com.zitao.common.utils.R;



/**
 * spu信息
 *
 * @author Zitao Wang
 * @email zitao.wang@tum.de
 * @date 2022-04-26 21:20:37
 */
@RestController
@RequestMapping("product/spuinfo")
public class SpuInfoController {
    @Autowired
    private SpuInfoService spuInfoService;

    /**
     * 商品上架
     * @return
     */
    @PostMapping("/{spuId}/up")
    public R upSpuForSearch(@PathVariable("spuId")Long spuId) {
        spuInfoService.upSpuForSearch(spuId);
        return R.ok();
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = spuInfoService.queryPageByCondition(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		SpuInfoEntity spuInfo = spuInfoService.getById(id);

        return R.ok().put("spuInfo", spuInfo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody SpuSaveVo vo){
		spuInfoService.saveSpuInfo(vo);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody SpuInfoEntity spuInfo){
		spuInfoService.updateById(spuInfo);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		spuInfoService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
