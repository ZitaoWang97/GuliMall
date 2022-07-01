package com.zitao.gulimall.product.app;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zitao.gulimall.product.entity.CategoryEntity;
import com.zitao.gulimall.product.service.CategoryService;
import com.zitao.common.utils.R;



/**
 * 商品三级分类
 *
 * @author Zitao Wang
 * @email zitao.wang@tum.de
 * @date 2022-04-26 21:20:37
 */
@RestController
@RequestMapping("product/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 查出所有分类以及子分类，并以树形结构组装
     */
    @RequestMapping("/list/tree")
    public R list(){
        List<CategoryEntity> entityList = categoryService.listWithTree();
        return R.ok().put("data", entityList);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{catId}")
    public R info(@PathVariable("catId") Long catId){
		CategoryEntity category = categoryService.getById(catId);

        return R.ok().put("data", category);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody CategoryEntity category){
		categoryService.save(category);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody CategoryEntity category){
		categoryService.updateCascade(category);

        return R.ok();
    }

    /**
     * 批量修改
     */
    @RequestMapping("/update/sort")
    public R updateSort(@RequestBody CategoryEntity[] category){
        categoryService.updateBatchById(Arrays.asList(category));
        return R.ok();
    }

    /**
     * 删除
     * @RequestBody:需要去获取请求体，必须发送post请求，获取json并且封装成对象
     * SpringMVC自动将请求体的数据（json）转为对应的对象
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] catIds){
        // 检查当前删除分类是否被别的地方引用
//		categoryService.removeByIds(Arrays.asList(catIds));
		categoryService.removeMenuByIds(Arrays.asList(catIds));

        return R.ok();
    }

}
