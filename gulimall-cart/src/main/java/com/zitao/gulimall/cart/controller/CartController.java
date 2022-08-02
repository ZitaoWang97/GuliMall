package com.zitao.gulimall.cart.controller;


import com.zitao.gulimall.cart.service.CartService;
import com.zitao.gulimall.cart.vo.CartItemVo;
import com.zitao.gulimall.cart.vo.CartVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class CartController {
    @Autowired
    private CartService cartService;

    /**
     * 获取用户购物车信息
     *
     * @param model
     * @return
     */
    @GetMapping("/cart.html")
    public String getCartList(Model model) {
        CartVo cartVo = cartService.getCart();
        model.addAttribute("cart", cartVo);
        return "cartList";
    }

    @RequestMapping("/success.html")
    public String success() {
        return "success";
    }

    /**
     * 添加商品到购物车
     * RedirectAttributes.addFlashAttribute(): 将数据放在session中，可以在页面中取出，但是只能取一次
     * RedirectAttributes.addAttribute(): 将数据放在url后面
     *
     * @return
     */
    @RequestMapping("/addCartItem")
    public String addCartItem(@RequestParam("skuId") Long skuId,
                              @RequestParam("num") Integer num,
                              RedirectAttributes attributes) {
        cartService.addCartItem(skuId, num);
        attributes.addAttribute("skuId", skuId);
        return "redirect:http://cart.gulimall.com/addCartItemSuccess";
    }

    /**
     * 重定向到成功页面 再次查询购物车的数据即可
     *
     * @param skuId
     * @param model
     * @return
     */
    @RequestMapping("/addCartItemSuccess")
    public String addCartItemSuccess(@RequestParam("skuId") Long skuId, Model model) {
        CartItemVo cartItemVo = cartService.getCartItem(skuId);
        model.addAttribute("cartItem", cartItemVo);
        return "success";
    }


    /**
     * 勾选购物项
     *
     * @param isChecked
     * @param skuId
     * @return
     */
    @GetMapping("/checkCart")
    public String checkCart(@RequestParam("check") Integer isChecked,
                            @RequestParam("skuId") Long skuId) {
        cartService.checkCart(skuId, isChecked);
        return "redirect:http://cart.gulimall.com/cart.html";
    }

    /**
     * 改变商品的数量
     *
     * @param skuId
     * @param num
     * @return
     */
    @RequestMapping("/countItem")
    public String changeItemCount(@RequestParam("skuId") Long skuId,
                                  @RequestParam("num") Integer num) {
        cartService.changeItemCount(skuId, num);
        return "redirect:http://cart.gulimall.com/cart.html";
    }

    /**
     * 删除商品
     *
     * @param skuId
     * @return
     */
    @RequestMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId) {
        cartService.deleteItem(skuId);
        return "redirect:http://cart.gulimall.com/cart.html";
    }

    /**
     * 获取购物车中勾选了的商品 添加到订单中
     *
     * @return
     */
    @ResponseBody
    @RequestMapping("/getCheckedItems")
    public List<CartItemVo> getCheckedItems() {
        return cartService.getCheckedItems();
    }
}
