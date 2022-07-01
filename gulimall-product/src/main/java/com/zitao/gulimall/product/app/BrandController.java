package com.zitao.gulimall.product.app;

import java.util.Arrays;
import java.util.Map;

import com.zitao.common.valid.AddGroup;
import com.zitao.common.valid.UpdateGroup;
import com.zitao.common.valid.UpdateStatusGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zitao.gulimall.product.entity.BrandEntity;
import com.zitao.gulimall.product.service.BrandService;
import com.zitao.common.utils.PageUtils;
import com.zitao.common.utils.R;


/**
 * 品牌
 *
 * @author Zitao Wang
 * @email zitao.wang@tum.de
 * @date 2022-04-26 21:20:37
 */
@RestController
@RequestMapping("product/brand")
public class BrandController {
    @Autowired
    private BrandService brandService;


    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = brandService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{brandId}")
    public R info(@PathVariable("brandId") Long brandId) {
        BrandEntity brand = brandService.getById(brandId);
        return R.ok().put("brand", brand);
    }

    /**
     * 保存
     * BindingResult result： 校验结果，可以拿到错误属性和对应的信息
     * 不标注的话，所有的异常都被SpringMVC统一集中处理
     * @Validated：分组校验
     */
    @RequestMapping("/save")
    public R save(@Validated(AddGroup.class) @RequestBody BrandEntity brand /**BindingResult result*/) {
//        if (result.hasErrors()) {
//            Map<String, String> map = new HashMap<>();
//            result.getFieldErrors().forEach((item) -> {
//                String defaultMessage = item.getDefaultMessage();
//                String field = item.getField();
//                map.put(field, defaultMessage);
//            });
//            return R.error(400, "提交数据不合法").put("data", map);
//        } else {
//            brandService.save(brand);
//            return R.ok();
//        }

        brandService.save(brand);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@Validated(UpdateGroup.class) @RequestBody BrandEntity brand) {
        brandService.updateDetail(brand);
        return R.ok();
    }

    /**
     * 修改状态
     */
    @RequestMapping("/update/status")
    public R updateStatus(@Validated(UpdateStatusGroup.class) @RequestBody BrandEntity brand) {
        brandService.updateById(brand);
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] brandIds) {
        brandService.removeByIds(Arrays.asList(brandIds));

        return R.ok();
    }

}
