package com.zitao.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.zitao.common.to.mq.SeckillOrderTo;
import com.zitao.common.utils.R;
import com.zitao.common.vo.MemberResponseVo;
import com.zitao.gulimall.seckill.service.SecKillService;
import com.zitao.gulimall.seckill.feign.CouponFeignService;
import com.zitao.gulimall.seckill.feign.ProductFeignService;
import com.zitao.gulimall.seckill.interceptor.LoginInterceptor;
import com.zitao.gulimall.seckill.to.SeckillSkuRedisTo;
import com.zitao.gulimall.seckill.vo.SeckillSessionWithSkusVo;
import com.zitao.gulimall.seckill.vo.SkuInfoVo;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service("SecKillService")
public class SecKillServiceImpl implements SecKillService {

    @Autowired
    private CouponFeignService couponFeignService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final String SESSION_CACHE_PREFIX = "seckill:sessions:";

    private final String SECKILL_CHARE_PREFIX = "seckill:skus";

    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:";


    /**
     * 在凌晨三点 悄咪咪地给缓存中上架秒杀
     */
    @Override
    public void uploadSeckillSkuLatest3Days() {
        // 1. 扫描最近三天需要参与秒杀的活动
        R r = couponFeignService.getSeckillSessionsIn3Days();
        if (r.getCode() == 0) {
            List<SeckillSessionWithSkusVo> sessions = r.getData(
                    new TypeReference<List<SeckillSessionWithSkusVo>>() {
                    });
            // 2. 在redis中分别保存秒杀场次信息和场次对应的秒杀商品信息
            saveSecKillSession(sessions);
            saveSecKillSku(sessions);
        }
    }

    /**
     * 查询当前时间可以参与秒杀的商品信息的集合
     *
     * @return
     */
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        /**
         * 保存时的key = "seckill:sessions:" + startTime + "_" + endTime
         */
        Set<String> keys = redisTemplate.keys(SESSION_CACHE_PREFIX + "*");
        long currentTime = System.currentTimeMillis();
        for (String key : keys) {
            String replace = key.replace(SESSION_CACHE_PREFIX, "");
            String[] split = replace.split("_");
            long startTime = Long.parseLong(split[0]);
            long endTime = Long.parseLong(split[1]);
            // 如果当前秒杀活动处于有效时间内
            if (currentTime > startTime && currentTime < endTime) {
                List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SECKILL_CHARE_PREFIX);
                List<SeckillSkuRedisTo> collect = range.stream().map(s -> {
                    String json = (String) ops.get(s);
                    SeckillSkuRedisTo redisTo = JSON.parseObject(json, SeckillSkuRedisTo.class);
                    return redisTo;
                }).collect(Collectors.toList());
                return collect;
            }
        }
        return null;
    }

    /**
     * 给定商品sku_id 查询是否在秒杀活动中
     *
     * @param skuId
     * @return
     */
    @Override
    public SeckillSkuRedisTo getSeckillSkuInfo(Long skuId) {
        BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(SECKILL_CHARE_PREFIX);
        Set<String> keys = ops.keys();
        for (String key : keys) {
            if (Pattern.matches("\\d-" + skuId, key)) {
                String v = ops.get(key);
                SeckillSkuRedisTo redisTo = JSON.parseObject(v, SeckillSkuRedisTo.class);
                // 当前商品参与秒杀活动
                if (redisTo != null) {
                    long current = System.currentTimeMillis();
                    // 当前活动在有效期，暴露商品随机码返回
                    if (redisTo.getStartTime() < current && redisTo.getEndTime() > current) {
                        return redisTo;
                    }
                    redisTo.setRandomCode(null);
                    return redisTo;
                }
            }
        }
        return null;
    }

    /**
     * 秒杀
     * @param killId
     * @param key
     * @param num
     * @return
     * @throws InterruptedException
     */
    @Override
    public String kill(String killId, String key, Integer num) throws InterruptedException {
        BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(SECKILL_CHARE_PREFIX);
        String json = ops.get(killId);
        String orderSn = null;
        if (!StringUtils.isEmpty(json)) {
            SeckillSkuRedisTo redisTo = JSON.parseObject(json, SeckillSkuRedisTo.class);
            //1. 验证时效
            long current = System.currentTimeMillis();
            if (current >= redisTo.getStartTime() && current <= redisTo.getEndTime()) {
                //2. 验证商品和商品随机码是否对应
                String redisKey = redisTo.getPromotionSessionId() + "-" + redisTo.getSkuId();
                if (redisKey.equals(killId) && redisTo.getRandomCode().equals(key)) {
                    //3. 验证当前用户是否购买过
                    MemberResponseVo memberResponseVo = LoginInterceptor.loginUser.get();
                    long ttl = redisTo.getEndTime() - System.currentTimeMillis();
                    //3.1 通过在redis中使用 用户id-skuId 来占位看是否买过
                    Boolean occupy = redisTemplate.opsForValue().setIfAbsent(memberResponseVo.getId() + "-" + redisTo.getSkuId(), num.toString(), ttl, TimeUnit.MILLISECONDS);
                    //3.2 占位成功，说明该用户未秒杀过该商品，则继续
                    if (occupy) {
                        //4. 校验库存和购买量是否符合要求
                        if (num <= redisTo.getSeckillLimit()) {
                            //4.1 尝试获取库存信号量
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + redisTo.getRandomCode());
                            boolean acquire = semaphore.tryAcquire(num, 100, TimeUnit.MILLISECONDS);
                            //4.2 获取库存成功
                            if (acquire) {
                                //5. 发送消息创建订单
                                //5.1 创建订单号
                                orderSn = IdWorker.getTimeId();
                                //5.2 创建秒杀订单to
                                SeckillOrderTo orderTo = new SeckillOrderTo();
                                orderTo.setMemberId(memberResponseVo.getId());
                                orderTo.setNum(num);
                                orderTo.setOrderSn(orderSn);
                                orderTo.setPromotionSessionId(redisTo.getPromotionSessionId());
                                orderTo.setSeckillPrice(redisTo.getSeckillPrice());
                                orderTo.setSkuId(redisTo.getSkuId());
                                //5.3 发送创建订单的消息
                                rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", orderTo);
                            }
                        }
                    }
                }
            }
            return orderSn;
        }
        return null;
    }

    /**
     * 缓存活动信息
     *
     * @param sessions
     */
    private void saveSecKillSession(List<SeckillSessionWithSkusVo> sessions) {
        sessions.stream().forEach(session -> {
            /**
             * key = "seckill:sessions:" + startTime + "_" + endTime
             */
            String key = SESSION_CACHE_PREFIX +
                    session.getStartTime().getTime() +
                    "_" + session.getEndTime().getTime();
            /**
             * value = List < promotion_session_id + "-" + sku_id >
             */
            if (!redisTemplate.hasKey(key)) {
                List<String> values = session.getRelations().stream()
                        .map(sku -> sku.getPromotionSessionId() + "-" + sku.getSkuId())
                        .collect(Collectors.toList());
                redisTemplate.opsForList().leftPushAll(key, values);
            }
        });
    }

    /**
     * 缓存活动的关联商品信息
     *
     * @param sessions
     */
    private void saveSecKillSku(List<SeckillSessionWithSkusVo> sessions) {
        // 1.1 绑定一个redis的哈希操作 key="seckill:skus"
        BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SECKILL_CHARE_PREFIX);
        sessions.stream().forEach(session -> {
            session.getRelations().stream().forEach(sku -> {
                // 1.2 哈希操作下的key = promotion_session_id + "-" + sku_id ; value = SeckillSkuRedisTo
                String key = sku.getPromotionSessionId() + "-" + sku.getSkuId();
                if (!ops.hasKey(key)) {
                    // 2. 缓存商品
                    SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();
                    // 2.1 拷贝SeckillSkuVo中的秒杀信息
                    BeanUtils.copyProperties(sku, redisTo);
                    // 2.2 保存开始结束时间
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());
                    // 2.3 远程查询sku信息并保存
                    R r = productFeignService.info(sku.getSkuId());
                    if (r.getCode() == 0) {
                        SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        redisTo.setSkuInfoVo(skuInfo);
                    }
                    // 2.4 生成商品随机码，防止恶意攻击
                    String token = UUID.randomUUID().toString().replace("-", "");
                    redisTo.setRandomCode(token);
                    // 2.5 序列化为json并保存
                    String jsonString = JSON.toJSONString(redisTo);
                    ops.put(key, jsonString);
                    // 2.6 使用库存作为Redisson信号量限制库存  key = "seckill:stock:" + token
                    // 因为不能频繁去查数据库 通过信号量来进行限流
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                    semaphore.trySetPermits(sku.getSeckillCount());
                }
            });
        });
    }
}
