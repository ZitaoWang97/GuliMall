package com.zitao.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.zitao.gulimall.product.service.CategoryBrandRelationService;
import com.zitao.gulimall.product.vo.Catalog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zitao.common.utils.PageUtils;
import com.zitao.common.utils.Query;

import com.zitao.gulimall.product.dao.CategoryDao;
import com.zitao.gulimall.product.entity.CategoryEntity;
import com.zitao.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {
    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedissonClient redissonClient;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );
        return new PageUtils(page);
    }

    /**
     * 获取三级分类
     *
     * @return
     */
    @Override
    public List<CategoryEntity> listWithTree() {
        // 1. 查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);
        // 2. 组装成父子树形结构
        // 2.1. 找到所有一级分类
        List<CategoryEntity> level1Menus = entities.stream().filter(categoryEntity ->
                        categoryEntity.getParentCid() == 0
                // 由{return categoryEntity.getParentCid() == 0;}省略而来
        ).map((menu) -> {
            menu.setChildren(getChildren(menu, entities));
            return menu;
        }).sorted((menu1, menu2) -> {
            // sort字段为Integer，可能为null
            return (menu1.getSort() == null ? 0 : menu1.getSort())
                    - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());
        return level1Menus;
    }

    /**
     * 递归查所有分类的子分类
     *
     * @param root
     * @param all
     * @return
     */
    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> children = all.stream().filter(categoryEntity ->
                        root.getCatId() == categoryEntity.getParentCid()
                // 当前分类id等于所有分类id里的parent_id时，说明找到。三级分类在这一层就全为false，返回null
        ).map(categoryEntity -> {
            // 递归寻找子菜单，直到三级
            categoryEntity.setChildren(getChildren(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            // 菜单的排序
            return (menu1.getSort() == null ? 0 : menu1.getSort())
                    - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());
        return children;
    }

    /**
     * 删除分类
     *
     * @param asList
     */
    @Override
    public void removeMenuByIds(List<Long> asList) {
        // TODO 检查当前删除分类是否被别的地方引用
        // 逻辑删除
        baseMapper.deleteBatchIds(asList);
    }


    /**
     * 级联更新关联表的分类信息
     *
     * 缓存 - 数据库 一致性的解决办法:
     * 1. 失效模式: 写完数据库后删缓存 也存在脏数据问题 都能通过加读写锁来规避  @CacheEvict
     * 2. 双写模式: 每次写完数据库之后都向缓存中重写 有暂时性的脏数据问题 最终一致性 @CachePut
     * 一旦三级菜单有更新，就直接把category分区的所有数据都清除
     *
     * @param category
     */
    @Override
    @Transactional
    @CacheEvict(value = {"category"}, allEntries = true)
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    /**
     * 获取所有一级分类
     *
     * @return
     * @Cacheable: 代表当前方法的结果需要进行缓存，如果缓存中有，方法不调用
     * 如果缓存中没有就会调用该方法，最后将方法返回的结果放入缓存
     * 每一个需要缓存的数据都要放入指定的分区中（value按照业务类型命名）
     */
    @Override
    @Cacheable(value = {"category"}, key = "#root.methodName")
    public List<CategoryEntity> getLevel1Categories() {
        List<CategoryEntity> parent_cid = this.list(new QueryWrapper<CategoryEntity>()
                .eq("parent_cid", 0));
        return parent_cid;
    }

    /**
     * 获取两级分类 with List<三级分类>
     *
     * 自定义spring cache:
     *  1. 采用json序列化机制 而不是默认的jdk
     *  2. key 以方法名为准
     *  3. 指定缓存的存活时间
     *
     * spring cache的不足:
     * 读模式:
     *  1. 缓存穿透: 查询一个null数据 解决: cache-null-values = true
     *  2. 缓存击穿: 大量并发进来同时查询一个正好过期的hot key 默认不加锁 解决: sync = true 开启本地锁 尚未加分布式锁
     *  3. 缓存雪崩: 大量的key同时过期 解决: 加过期时间
     * 写模式:
     *  1. 读写加锁
     *  2. 引入Canal 感知MySQL的更新去更新数据库
     *  3. 读多写少的数据直接去查数据库 不需要加入缓存
     * @return
     */
    @Cacheable(value = {"category"}, key = "#root.methodName", sync = true)
    @Override
    public Map<String, List<Catalog2Vo>> getCatalogJsonDbWithSpringCache() {
        return getCategoriesDb();
    }

    /**
     * 通过redisson来加分布式锁
     *
     * @return
     */
    public Map<String, List<Catalog2Vo>> getCatalogJsonDbWithRedisson() {
        Map<String, List<Catalog2Vo>> categoryMap = null;
        RLock lock = redissonClient.getLock("CatalogJson-Lock");
        lock.lock(30, TimeUnit.SECONDS);
        try {
            categoryMap = getCategoriesDb();
        } finally {
            lock.unlock();
        }
        return categoryMap;
    }

    /**
     * 通过redis占坑来尝试分布式锁
     * 问题：
     * 1. 加锁后设置过期时间以避免死锁问题
     * 2. 加锁和设置过期时间必须是一个原子操作 否则在两种中间程序宕机也会产生死锁
     * 3. 由于业务时间很长，锁已经过期自动删除，有可能删除别的线程正在持有的锁 -> 锁的值指定为uuid
     * 4. 去redis中查询uuid需要一定的网络通信时间，有可能在拿到uuid的同时锁过期了 -> 获取锁+对比uuid值删锁 原子操作
     * (redis支持相同的lua解释器，来运行所有的命令，并且保证脚本以原子方式执行，期间不会执行其他脚本或者redis命令)
     *
     * @return
     */
    public Map<String, List<Catalog2Vo>> getCatalogJsonDbWithRedisLock() {
        String uuid = UUID.randomUUID().toString();
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        // 1/2/3. 给锁要设置过期时间以防止死锁 并且加锁和设置过期时间是一个原子性事件 uuid能确保删的是自己的锁
        Boolean lock = ops.setIfAbsent("lock", uuid, 10, TimeUnit.SECONDS);
        if (lock) {
            System.out.println("获取分布式锁成功...");
            Map<String, List<Catalog2Vo>> categoriesDb = getCategoriesDb();
            // 4. redis+lua脚本保证 [获取uuid值对比+对比成功删除锁] 是一个原子性操作
            String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1] then\n" +
                    "    return redis.call(\"del\",KEYS[1])\n" +
                    "else\n" +
                    "    return 0\n" +
                    "end";
            stringRedisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                    Arrays.asList("lock"), uuid);
            return categoriesDb;
        } else {
            // 占锁失败 休眠100ms后重试（自旋的方式）
            try {
                System.out.println("获取分布式锁失败，sleep100ms后重试...");
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return getCatalogJsonDbWithRedisLock();
        }
    }

    /**
     * 查询redis中是否存在数据，否则就查询数据库
     * TODO 堆外内存溢出的异常
     * 原因分析: springboot2.0后默认使用Lettuce作为操作redis的客户端
     * 它使用netty作为网络通信 Lettuce的bug导致netty堆外内存溢出
     * netty如果没有指定堆外内存 默认使用-Xmx 没有进行内存释放
     * 解决方法: 1. 升级lettuce客户端  2. 切换jedis客户端
     * spring对lettuce和jedis进一步封装为RedisTemplate
     *
     * @return
     */
    public Map<String, List<Catalog2Vo>> getCategoryMap() {
        // 1. 缓存中存的是json字符串 json跨语言跨平台兼容
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        String catalogJson = ops.get("catalogJson");
        if (StringUtils.isEmpty(catalogJson)) {
            System.out.println("缓存不命中，准备查询数据库");
            // 查询数据库 并放入redis中
            Map<String, List<Catalog2Vo>> categoriesDb = getCategoriesDb();
            return categoriesDb;
        }
        System.out.println("缓存命中");
        // 2. 将json字符串逆转为能用的对象类型 - 反序列化过程
        Map<String, List<Catalog2Vo>> listMap = JSON.parseObject(catalogJson,
                new TypeReference<Map<String, List<Catalog2Vo>>>() {
                });
        return listMap;
    }

    /**
     * 查询数据库并且封装一整个三级分类的数据
     * 锁的时序问题: 确认redis中无数据 - 查询数据库 - 给redis中放入数据 应当是一个原子性事件
     *
     * @return
     */
    private Map<String, List<Catalog2Vo>> getCategoriesDb() {
        // 1. 确认redis中无数据
//        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
//        String catalogJson = ops.get("catalogJson");
//        if (!StringUtils.isEmpty(catalogJson)) {
//            Map<String, List<Catalog2Vo>> result = JSON.parseObject(catalogJson,
//                    new TypeReference<Map<String, List<Catalog2Vo>>>() {
//                    });
//            return result;
//        }

        // 2. 查询数据库
        System.out.println("查询了数据库");
        // 优化业务逻辑，仅查询一次数据库，获取所有分类的信息（1、2、3级）
        List<CategoryEntity> categoryEntities = this.list();
        // 查出所有一级分类
        List<CategoryEntity> level1Categories = getCategoryByParentCid(categoryEntities, 0L);
        Map<String, List<Catalog2Vo>> listMap = level1Categories.stream().collect(
                Collectors.toMap(k -> k.getCatId().toString(), v -> {
                    // 遍历查找出二级分类
                    List<CategoryEntity> level2Categories = getCategoryByParentCid(categoryEntities, v.getCatId());
                    List<Catalog2Vo> catalog2Vos = null;
                    if (level2Categories != null) {
                        // 封装二级分类到vo并且查出其中的三级分类
                        catalog2Vos = level2Categories.stream().map(l2 -> {
                            // 遍历查出三级分类并封装
                            List<CategoryEntity> level3Catagories = getCategoryByParentCid(categoryEntities, l2.getCatId());
                            List<Catalog2Vo.Catalog3Vo> catalog3Vos = null;
                            if (level3Catagories != null) {
                                catalog3Vos = level3Catagories.stream()
                                        .map(l3 -> new Catalog2Vo.Catalog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName()))
                                        .collect(Collectors.toList());
                            }
                            Catalog2Vo catalog2Vo = new Catalog2Vo(v.getCatId().toString(), l2.getCatId().toString(), l2.getName(), catalog3Vos);
                            return catalog2Vo;
                        }).collect(Collectors.toList());
                    }
                    return catalog2Vos;
                }));
        // 3. 给redis中放入数据
//        String toJSONString = JSON.toJSONString(listMap);
//        ops.set("catalogJson", toJSONString);
        return listMap;
    }

    private List<CategoryEntity> getCategoryByParentCid(List<CategoryEntity> categoryEntities, long parentCid) {
        List<CategoryEntity> collect = categoryEntities.stream()
                .filter(cat -> cat.getParentCid() == parentCid)
                .collect(Collectors.toList());
        return collect;
    }

    /**
     * 找到完整的三级分类路径 [1级id,2级id,3级id]
     *
     * @param catalogId
     * @return
     */
    @Override
    public Long[] findCategoryPath(Long catalogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catalogId, paths);
        Collections.reverse(parentPath);
        return parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * 递归找父类class id并包装在List中
     *
     * @param catalogId
     * @param paths
     * @return
     */
    public List<Long> findParentPath(Long catalogId, List<Long> paths) {
        // 先收集自身的分类id
        paths.add(catalogId);
        CategoryEntity byId = this.getById(catalogId);
        // 如果有父节点，就继续向上收集分类id
        if (byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;
    }

}