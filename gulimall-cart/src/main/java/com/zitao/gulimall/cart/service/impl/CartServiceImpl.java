package com.zitao.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import com.zitao.common.constant.CartConstant;
import com.zitao.common.utils.R;
import com.zitao.gulimall.cart.feign.ProductFeignService;
import com.zitao.gulimall.cart.interceptor.CartInterceptor;
import com.zitao.gulimall.cart.service.CartService;
import com.zitao.gulimall.cart.to.UserInfoTo;
import com.zitao.gulimall.cart.vo.CartItemVo;
import com.zitao.gulimall.cart.vo.CartVo;
import com.zitao.gulimall.cart.vo.SkuInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service("CartService")
public class CartServiceImpl implements CartService {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private ThreadPoolExecutor executor;

    /**
     * 将商品添加至购物车
     *
     * @param skuId
     * @param num
     * @return
     */
    @Override
    public CartItemVo addCartItem(Long skuId, Integer num) {
        BoundHashOperations<String, Object, Object> ops = getCartItemOps();
        // 判断当前商品是否已经在购物车中
        String cartJson = (String) ops.get(skuId.toString());
        // 1. 已经存在购物车，将数据取出并添加商品数量
        if (!StringUtils.isEmpty(cartJson)) {
            // 1.1 将json转为对象并增添数量
            CartItemVo cartItemVo = JSON.parseObject(cartJson, CartItemVo.class);
            cartItemVo.setCount(cartItemVo.getCount() + num);
            // 1.2 将更新后的对象转为json并存入redis
            String jsonString = JSON.toJSONString(cartItemVo);
            ops.put(skuId.toString(), jsonString);
            return cartItemVo;
        } else {
            // 2. 未存在购物车，则添加新商品
            CartItemVo cartItemVo = new CartItemVo();
            CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
                // 2.1 异步任务1: 远程查询sku基本信息
                R info = productFeignService.info(skuId);
                SkuInfoVo skuInfo = info.getData("skuInfo",
                        new TypeReference<SkuInfoVo>() {
                        });
                cartItemVo.setCheck(true);
                cartItemVo.setCount(num);
                cartItemVo.setImage(skuInfo.getSkuDefaultImg());
                cartItemVo.setPrice(skuInfo.getPrice());
                cartItemVo.setSkuId(skuId);
                cartItemVo.setTitle(skuInfo.getSkuTitle());
            }, executor);

            // 2.2 异步任务2: 远程查询sku销售属性组合信息
            CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
                List<String> attrValuesAsString = productFeignService.getSkuSaleAttrValuesAsString(skuId);
                cartItemVo.setSkuAttrValues(attrValuesAsString);
            }, executor);

            // 2.3 等异步任务都完成后才能放数据
            try {
                CompletableFuture.allOf(future1, future2).get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            // 2.4 将该属性封装并存入redis,登录用户使用userId为key,否则使用user-key
            String toJSONString = JSON.toJSONString(cartItemVo);
            ops.put(skuId.toString(), toJSONString);
            return cartItemVo;
        }
    }

    @Override
    public CartItemVo getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartItemOps = getCartItemOps();
        String s = (String) cartItemOps.get(skuId.toString());
        CartItemVo cartItemVo = JSON.parseObject(s, CartItemVo.class);
        return cartItemVo;
    }

    /**
     * 查看购物车信息
     * <p>
     * 浏览器有cookie user-key: 标识用户身份
     * 如果第一次访问购物车功能，都会给一个临时的用户身份
     * 浏览器以后保存，每次访问都会带上这个cookie
     * <p>
     * 1. 用户已登录: session中有
     * 2. 没登录: 按照cookie里带的user-key来
     * 3. 第一次: 如果没有临时用户，帮忙创建
     *
     * @return
     */
    @Override
    public CartVo getCart() {
        CartVo cartVo = new CartVo();
        // 从拦截器中获取用户是否登录的信息
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        // 1. 用户未登录，直接通过user-key获取临时购物车
        List<CartItemVo> tempCart = getCartByKey(userInfoTo.getUserKey());
        if (StringUtils.isEmpty(userInfoTo.getUserId())) {
            List<CartItemVo> cartItemVos = tempCart;
            cartVo.setItems(cartItemVos);
        } else {
            // 2 用户登录
            // 2.1 查询user-key对应的临时购物车，并和用户购物车合并
            if (tempCart != null && tempCart.size() > 0) {
                for (CartItemVo cartItemVo : tempCart) {
                    // 2.2 在redis中更新数据
                    addCartItem(cartItemVo.getSkuId(), cartItemVo.getCount());
                }
            }
            // 2.3 查询userID对应的购物车 这时临时购物车里的内容已经合并完成
            cartVo.setItems(getCartByKey(userInfoTo.getUserId().toString()));
            // 2.4 删除临时购物车数据
            redisTemplate.delete(CartConstant.CART_PREFIX + userInfoTo.getUserKey());
        }

        return cartVo;
    }

    /**
     * 勾选商品
     *
     * @param skuId
     * @param isChecked
     */
    @Override
    public void checkCart(Long skuId, Integer isChecked) {
        BoundHashOperations<String, Object, Object> ops = getCartItemOps();
        String cartJson = (String) ops.get(skuId.toString());
        CartItemVo cartItemVo = JSON.parseObject(cartJson, CartItemVo.class);
        cartItemVo.setCheck(isChecked == 1);
        ops.put(skuId.toString(), JSON.toJSONString(cartItemVo));
    }

    /**
     * 改变商品数量
     *
     * @param skuId
     * @param num
     */
    @Override
    public void changeItemCount(Long skuId, Integer num) {
        BoundHashOperations<String, Object, Object> ops = getCartItemOps();
        String cartJson = (String) ops.get(skuId.toString());
        CartItemVo cartItemVo = JSON.parseObject(cartJson, CartItemVo.class);
        cartItemVo.setCount(num);
        ops.put(skuId.toString(), JSON.toJSONString(cartItemVo));
    }

    /**
     * 删除商品
     *
     * @param skuId
     */
    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> ops = getCartItemOps();
        ops.delete(skuId.toString());
    }

    /**
     * 获取勾选商品
     *
     * @return
     */
    @Override
    public List<CartItemVo> getCheckedItems() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        // TODO 更新价格为商品的最新价格
        List<CartItemVo> cartByKey = getCartByKey(userInfoTo.getUserId().toString());
        return cartByKey.stream().filter(CartItemVo::getCheck).collect(Collectors.toList());
    }

    private List<CartItemVo> getCartByKey(String userKey) {
        BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(
                CartConstant.CART_PREFIX + userKey);

        List<Object> values = ops.values();
        if (values != null && values.size() > 0) {
            List<CartItemVo> cartItemVos = values.stream().map(obj -> {
                String json = (String) obj;
                return JSON.parseObject(json, CartItemVo.class);
            }).collect(Collectors.toList());
            return cartItemVos;
        }
        return null;
    }

    /**
     * 获取到要操作的购物车
     *
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartItemOps() {
        // 获取到拦截器的返回数据
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        // 1. userId不为空 则说明用户已经登录 userId操作redis
        if (!StringUtils.isEmpty(userInfoTo.getUserId())) {
            // gulimall:cart: + userId   boundHashOps方法绑定一个redis的哈希操作
            return redisTemplate.boundHashOps(CartConstant.CART_PREFIX + userInfoTo.getUserId());
        } else {
            // 2. 用户未登录则使用user-key操作redis
            // gulimall:cart: + user-key
            return redisTemplate.boundHashOps(CartConstant.CART_PREFIX + userInfoTo.getUserKey());
        }
    }
}
