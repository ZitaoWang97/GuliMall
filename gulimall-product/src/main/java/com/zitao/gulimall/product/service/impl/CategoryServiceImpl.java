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

    @Override
    public void removeMenuByIds(List<Long> asList) {
        // TODO 检查当前删除菜单是否被别的地方引用

        // 这个是直接删除，用的不多；一般多用逻辑删除
        baseMapper.deleteBatchIds(asList);
    }


    /**
     * 级联更新关联表的分类信息
     * 缓存失效模式：一旦三级菜单有更新，就直接把category分区的所有数据都清除
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
     * 代表当前方法的结果需要进行缓存，如果缓存中有，方法不调用，如果缓存中没有就会调用该方法，最后将方法返回的结果放入缓存
     * 每一个需要缓存的数据都要放入指定的分区中（value按照业务类型命名）
     * @return
     */
    @Override
    @Cacheable(value = {"category"}, key = "#root.methodName")
    public List<CategoryEntity> getLevel1Categories() {
        System.out.println("getLevel1Categories方法被调用");
        List<CategoryEntity> parent_cid = this.list(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return parent_cid;
    }

    @Cacheable(value = {"category"}, key = "#root.methodName", sync = true) // 开启本地锁，尚未加分布式锁
    @Override
    public Map<String, List<Catalog2Vo>> getCatalogJsonDbWithSpringCache() {
        return getCategoriesDb();
    }

    public Map<String, List<Catalog2Vo>> getCatalogJsonDbWithRedisson() {
        Map<String, List<Catalog2Vo>> categoryMap = null;
        RLock lock = redissonClient.getLock("CatalogJson-Lock");
        lock.lock(10, TimeUnit.SECONDS);
        try {
            categoryMap = getCategoriesDb();
        } finally {
            lock.unlock();
        }
        return categoryMap;
    }

    /**
     * 通过redis占坑来尝试分布式锁
     *
     * @return
     */
    public Map<String, List<Catalog2Vo>> getCatalogJsonDbWithRedisLock() {
        String uuid = UUID.randomUUID().toString();
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        // 给锁要设置过期时间以防止死锁，并且加锁和设置过期时间是一个原子性事件
        // uuid能确保删的是自己的锁
        Boolean lock = ops.setIfAbsent("lock", uuid, 10, TimeUnit.SECONDS);
        if (lock) {
            System.out.println("获取分布式锁成功...");
            // 占到分布式锁
            Map<String, List<Catalog2Vo>> categoriesDb = getCategoriesDb();
            // redis+lua脚本保证 [获取值对比+对比成功删除锁] 是一个原子性操作
            String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1] then\n" +
                    "    return redis.call(\"del\",KEYS[1])\n" +
                    "else\n" +
                    "    return 0\n" +
                    "end";
            stringRedisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                    Arrays.asList("lock"), uuid);
            return categoriesDb;
        } else {
            // 占锁失败... 休眠100ms后重试（自旋的方式）
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
     * TODO 堆外内存溢出的异常 Lettuce的bug导致netty堆外内存溢出
     *
     * @return
     */
    public Map<String, List<Catalog2Vo>> getCategoryMap() {
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        String catalogJson = ops.get("catalogJson");
        if (StringUtils.isEmpty(catalogJson)) {
            System.out.println("缓存不命中，准备查询数据库");
            // 查询数据库
            Map<String, List<Catalog2Vo>> categoriesDb = getCategoriesDb();
            return categoriesDb;
        }
        System.out.println("缓存命中");
        // 将json字符串逆转为能用的对象类型：反序列化过程
        Map<String, List<Catalog2Vo>> listMap = JSON.parseObject(catalogJson,
                new TypeReference<Map<String, List<Catalog2Vo>>>() {
                });
        return listMap;
    }

    /**
     * 查询数据库
     * 确认resis中无数据-查询数据库-给redis中放入数据 应当是一个 原子性事件
     * @return
     */
    private Map<String, List<Catalog2Vo>> getCategoriesDb() {
//        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
//        String catalogJson = ops.get("catalogJson");
//        if (!StringUtils.isEmpty(catalogJson)) {
//            Map<String, List<Catalog2Vo>> result = JSON.parseObject(catalogJson,
//                    new TypeReference<Map<String, List<Catalog2Vo>>>() {
//                    });
//            return result;
//        }
        System.out.println("查询了数据库");
        //优化业务逻辑，仅查询一次数据库，获取所有分类的信息（1、2、3级）
        List<CategoryEntity> categoryEntities = this.list();
        //查出所有一级分类
        List<CategoryEntity> level1Categories = getCategoryByParentCid(categoryEntities, 0L);
        Map<String, List<Catalog2Vo>> listMap = level1Categories.stream().collect(
                Collectors.toMap(k -> k.getCatId().toString(), v -> {
                    //遍历查找出二级分类
                    List<CategoryEntity> level2Categories = getCategoryByParentCid(categoryEntities, v.getCatId());
                    List<Catalog2Vo> catalog2Vos = null;
                    if (level2Categories != null) {
                        //封装二级分类到vo并且查出其中的三级分类
                        catalog2Vos = level2Categories.stream().map(l2 -> {
                            //遍历查出三级分类并封装
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
        // 放入redis中 JSON跨语言跨平台兼容，给缓存中放入的都是JSON字符串：序列化过程
//        String toJSONString = JSON.toJSONString(listMap);
//        ops.set("catalogJson", toJSONString);
        return listMap;
    }

    private List<CategoryEntity> getCategoryByParentCid(List<CategoryEntity> categoryEntities, long parentCid) {
        List<CategoryEntity> collect = categoryEntities.stream().filter(cat -> cat.getParentCid() == parentCid)
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