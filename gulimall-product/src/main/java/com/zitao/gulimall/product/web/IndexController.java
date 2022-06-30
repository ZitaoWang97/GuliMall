package com.zitao.gulimall.product.web;


import com.zitao.gulimall.product.entity.CategoryEntity;
import com.zitao.gulimall.product.service.CategoryService;
import com.zitao.gulimall.product.vo.Catalog2Vo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class IndexController {
    @Autowired
    private CategoryService categoryService;

//    @Autowired
//    private RedissonClient redissonClient;
//
//    @Autowired
//    private StringRedisTemplate redisTemplate;
//
//    @Autowired
//    private SeckillFeignService seckillFeignService;

    @GetMapping({"/", "index.html"})
    public String getIndex(Model model) {
        //获取所有的一级分类
        List<CategoryEntity> categories = categoryService.getLevel1Categories();
        model.addAttribute("categories", categories);
        return "index";
    }

    @GetMapping("index/json/catalog.json")
    @ResponseBody
    public Map<String, List<Catalog2Vo>> getCategoryMap() {
        return categoryService.getCatalogJsonDbWithSpringCache();
    }

//    @GetMapping("/read")
//    @ResponseBody
//    public String read() {
//        RReadWriteLock lock = redissonClient.getReadWriteLock("ReadWrite-Lock");
//        RLock rLock = lock.readLock();
//        String s = "";
//        try {
//            rLock.lock();
//            System.out.println("读锁加锁"+Thread.currentThread().getId());
//            Thread.sleep(5000);
//            s= redisTemplate.opsForValue().get("lock-value");
//        }finally {
//            rLock.unlock();
//            return "读取完成:"+s;
//        }
//    }
//
//    @GetMapping("/write")
//    @ResponseBody
//    public String write() {
//        RReadWriteLock lock = redissonClient.getReadWriteLock("ReadWrite-Lock");
//        RLock wLock = lock.writeLock();
//        String s = UUID.randomUUID().toString();
//        try {
//            wLock.lock();
//            System.out.println("写锁加锁"+Thread.currentThread().getId());
//            Thread.sleep(10000);
//            redisTemplate.opsForValue().set("lock-value",s);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }finally {
//            wLock.unlock();
//            return "写入完成:"+s;
//        }
//    }
//
//    @GetMapping("/park")
//    @ResponseBody
//    public String park() {
//        RSemaphore park = redissonClient.getSemaphore("park");
//        try {
//            park.acquire(2);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        return "停进2";
//    }
//
//    @GetMapping("/go")
//    @ResponseBody
//    public String go() {
//        RSemaphore park = redissonClient.getSemaphore("park");
//        park.release(2);
//        return "开走2";
//    }
//
//    @GetMapping("/setLatch")
//    @ResponseBody
//    public String setLatch() {
//        RCountDownLatch latch = redissonClient.getCountDownLatch("CountDownLatch");
//        try {
//            latch.trySetCount(5);
//            latch.await();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        return "门栓被放开";
//    }
//
//    @GetMapping("/offLatch")
//    @ResponseBody
//    public String offLatch() {
//        RCountDownLatch latch = redissonClient.getCountDownLatch("CountDownLatch");
//        latch.countDown();
//        return "门栓被放开1";
//    }
//
//    @ResponseBody
//    @GetMapping("/getSeckillSkuInfo/{skuId}")
//    public R getSeckillSkuInfo(@PathVariable("skuId") Long skuId) {
//        return seckillFeignService.getSeckillSkuInfo(skuId);
//    }

}
