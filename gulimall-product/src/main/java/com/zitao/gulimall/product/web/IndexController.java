package com.zitao.gulimall.product.web;


import com.zitao.gulimall.product.entity.CategoryEntity;
import com.zitao.gulimall.product.service.CategoryService;
import com.zitao.gulimall.product.vo.Catalog2Vo;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller
public class IndexController {
    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

//    @Autowired
//    private SeckillFeignService seckillFeignService;

    /**
     * 查询所有的一级分类
     *
     * @param model
     * @return
     */
    @GetMapping({"/", "/index.html"})
    public String getIndex(Model model) {
        // 获取所有的一级分类
        List<CategoryEntity> categories = categoryService.getLevel1Categories();
        model.addAttribute("categories", categories);
        // 视图解析器进行拼串: classpath:/templates/ + 返回值 + .html
        return "index";
    }

    /**
     * 查询所有的二级分类（with List<三级分类>）
     *
     * @return
     */
    @GetMapping("/index/json/catalog.json")
    @ResponseBody
    public Map<String, List<Catalog2Vo>> getCategoryMap() {
        return categoryService.getCatalogJsonDbWithSpringCache();
    }


    /**
     * 测试redisson lock为可重入锁
     *
     * @return
     */
    @ResponseBody
    @GetMapping("/redissionTest")
    public String redissionTest() {
        RLock lock = redissonClient.getLock("redisson-test");
        // 阻塞式等待，默认加锁30s，不用像redis一样自旋调用
        // 如果业务时间很长，会自动续期，在运行期间自动续上新的30s
        // 业务一旦运行完成，就不会给当前锁续期，即使不手动解锁，锁默认在30s后自动删除
        lock.lock();
        // 如果传递了锁的超时时间，到时后就会自动解锁不会续期
        lock.lock(5, TimeUnit.SECONDS);
        try {
            System.out.println("加锁成功，执行业务中...线程号为：" + Thread.currentThread().getId());
            Thread.sleep(10000);
        } catch (Exception e) {

        } finally {
            System.out.println("释放锁...线程号为：" + Thread.currentThread().getId());
            lock.unlock();
        }
        return "redisson-test";
    }

    /**
     * 测试读写锁
     *
     * @return
     */
    @GetMapping("/read")
    @ResponseBody
    public String read() {
        RReadWriteLock lock = redissonClient.getReadWriteLock("ReadWrite-Lock");
        RLock rLock = lock.readLock();
        String s = "";
        try {
            rLock.lock();
            System.out.println("读锁加锁" + Thread.currentThread().getId());
            Thread.sleep(5000);
            s = redisTemplate.opsForValue().get("lock-value");
        } finally {
            rLock.unlock();
            System.out.println("读锁解锁" + Thread.currentThread().getId());
            return "读取完成:" + s;
        }
    }

    @GetMapping("/write")
    @ResponseBody
    public String write() {
        RReadWriteLock lock = redissonClient.getReadWriteLock("ReadWrite-Lock");
        RLock wLock = lock.writeLock();
        String s = UUID.randomUUID().toString();
        try {
            wLock.lock();
            System.out.println("写锁加锁" + Thread.currentThread().getId());
            // 模拟数据写入过程 10s
            Thread.sleep(10000);
            redisTemplate.opsForValue().set("lock-value", s);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            wLock.unlock();
            System.out.println("写锁解锁" + Thread.currentThread().getId());
            return "写入完成:" + s;
        }
    }

    /**
     * 测试信号量 Semaphore
     * 信号量可以用来分布式限流
     *
     * @return
     */
    @GetMapping("/park")
    @ResponseBody
    public String park() {
        RSemaphore park = redissonClient.getSemaphore("park");
        try {
            // 占用两个信号
            park.acquire(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "停进";
    }

    @GetMapping("/go")
    @ResponseBody
    public String go() {
        RSemaphore park = redissonClient.getSemaphore("park");
        // 释放两个信号
        park.release(2);
        return "开走";
    }


    /**
     * 测试闭锁Latch
     *
     * @return
     */
    @GetMapping("/setLatch")
    @ResponseBody
    public String setLatch() {
        RCountDownLatch latch = redissonClient.getCountDownLatch("CountDownLatch");
        try {
            latch.trySetCount(5);
            // 等待闭锁都完成
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "门栓被放开";
    }

    @GetMapping("/offLatch/{id}")
    @ResponseBody
    public String offLatch(@PathVariable("id") Long id) {
        RCountDownLatch latch = redissonClient.getCountDownLatch("CountDownLatch");
        latch.countDown();
        return "门栓被放开" + id;
    }

//    @ResponseBody
//    @GetMapping("/getSeckillSkuInfo/{skuId}")
//    public R getSeckillSkuInfo(@PathVariable("skuId") Long skuId) {
//        return seckillFeignService.getSeckillSkuInfo(skuId);
//    }

}
