package com.zitao.gulimall.order.controller;

import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.zitao.gulimall.order.entity.OrderEntity;
import com.zitao.gulimall.order.service.OrderService;
import com.zitao.common.utils.PageUtils;
import com.zitao.common.utils.R;



/**
 * 订单
 *
 * @author Zitao Wang
 * @email zitao.wang@tum.de
 * @date 2022-04-27 14:40:37
 */
@RestController
@RequestMapping("order/order")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @RequestMapping("/infoByOrderSn/{OrderSn}")
    public R infoByOrderSn(@PathVariable("OrderSn") String OrderSn){
        OrderEntity order = orderService.getOrderByOrderSn(OrderSn);
        return R.ok().put("order", order);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = orderService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		OrderEntity order = orderService.getById(id);

        return R.ok().put("order", order);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody OrderEntity order){
		orderService.save(order);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody OrderEntity order){
		orderService.updateById(order);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		orderService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

    @PostMapping("listWithItem")
    public R listWithItem(@RequestBody Map<String, Object> params) {
        PageUtils page = orderService.queryPageWithItem(params);
        return R.ok().put("page", page);
    }

}
