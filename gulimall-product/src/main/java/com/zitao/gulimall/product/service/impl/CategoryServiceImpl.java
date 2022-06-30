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
        ).map((menu) -> {
            menu.setChildren(getChildren(menu, entities));
            return menu;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());
        return level1Menus;
    }

    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map(categoryEntity -> {
            // 递归寻找子菜单，直到三级菜单
            categoryEntity.setChildren(getChildren(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            // 菜单的排序
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());
        return children;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        // TODO 检查当前删除菜单是否被别的地方引用
        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] findCategoryPath(Long catalogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catalogId, paths);
        Collections.reverse(parentPath);
        return parentPath.toArray(new Long[parentPath.size()]);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"category"},allEntries = true)
//    @Caching(evict = {
//            @CacheEvict(value = {"category"},key = "getLevel1Categories"),
//            @CacheEvict(value = {"category"},key = "getCatalogJsonDbWithSpringCache")
//    })
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    @Override
    @Cacheable(value = {"category"}, key = "#root.methodName")
    public List<CategoryEntity> getLevel1Categories() {
        List<CategoryEntity> parent_cid = this.list(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return parent_cid;
    }

    public Map<String, List<Catalog2Vo>> getCatalogJsonDbWithRedisson() {
        Map<String, List<Catalog2Vo>> categoryMap = null;
        RLock lock = redissonClient.getLock("CatalogJson-Lock");
        lock.lock(10, TimeUnit.SECONDS);
        try {
            categoryMap = getCategoryMap();
        }  finally {
            lock.unlock();
            return categoryMap;
        }
    }

    @Cacheable(value = {"category"}, key = "#root.methodName", sync = true)
    @Override
    public Map<String, List<Catalog2Vo>> getCatalogJsonDbWithSpringCache() {
        return getCategoriesDb();
    }

    /**
     * 通过redis占坑来尝试分布式锁
     *
     * @return
     */
    public Map<String, List<Catalog2Vo>> getCatalogJsonDbWithRedisLock() {
        String uuid = UUID.randomUUID().toString();
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        Boolean lock = ops.setIfAbsent("lock", uuid, 5, TimeUnit.SECONDS);
        if (lock) {
            // 占到分布式锁
            Map<String, List<Catalog2Vo>> categoriesDb = getCategoryMap();
            String lockValue = ops.get("lock");
            String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1] then\n" +
                    "    return redis.call(\"del\",KEYS[1])\n" +
                    "else\n" +
                    "    return 0\n" +
                    "end";
            stringRedisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList("lock"), lockValue);
            return categoriesDb;
        } else {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return getCatalogJsonDbWithRedisLock();
        }
    }

    /**
     * 查询redis中是否存在数据，否则就查询数据库
     * @return
     */
    public Map<String, List<Catalog2Vo>> getCategoryMap() {
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        String catalogJson = ops.get("catalogJson");
        if (StringUtils.isEmpty(catalogJson)) {
            System.out.println("缓存不命中，准备查询数据库");
            // 查询数据库
            Map<String, List<Catalog2Vo>> categoriesDb = getCategoriesDb();
            String toJSONString = JSON.toJSONString(categoriesDb);
            // 放入redis中
            ops.set("catalogJson", toJSONString);
            return categoriesDb;
        }
        System.out.println("缓存命中");
        Map<String, List<Catalog2Vo>> listMap = JSON.parseObject(catalogJson,
                new TypeReference<Map<String, List<Catalog2Vo>>>() {
                });
        return listMap;
    }

    /**
     * 查询数据库
     * @return
     */
    private Map<String, List<Catalog2Vo>> getCategoriesDb() {
        System.out.println("查询了数据库");
        //优化业务逻辑，仅查询一次数据库，获取所有分类的信息（1、2、3级）
        List<CategoryEntity> categoryEntities = this.list();
        //查出所有一级分类
        List<CategoryEntity> level1Categories = getCategoryByParentCid(categoryEntities, 0L);
        Map<String, List<Catalog2Vo>> listMap = level1Categories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //遍历查找出二级分类
            List<CategoryEntity> level2Categories = getCategoryByParentCid(categoryEntities, v.getCatId());
            List<Catalog2Vo> catalog2Vos = null;
            if (level2Categories != null) {
                //封装二级分类到vo并且查出其中的三级分类
                catalog2Vos = level2Categories.stream().map(cat -> {
                    //遍历查出三级分类并封装
                    List<CategoryEntity> level3Catagories = getCategoryByParentCid(categoryEntities, cat.getCatId());
                    List<Catalog2Vo.Catalog3Vo> catalog3Vos = null;
                    if (level3Catagories != null) {
                        catalog3Vos = level3Catagories.stream()
                                .map(level3 -> new Catalog2Vo.Catalog3Vo(level3.getParentCid().toString(), level3.getCatId().toString(), level3.getName()))
                                .collect(Collectors.toList());
                    }
                    Catalog2Vo catalog2Vo = new Catalog2Vo(v.getCatId().toString(), cat.getCatId().toString(), cat.getName(), catalog3Vos);
                    return catalog2Vo;
                }).collect(Collectors.toList());
            }
            return catalog2Vos;
        }));
        return listMap;
    }

    private List<CategoryEntity> getCategoryByParentCid(List<CategoryEntity> categoryEntities, long parentCid) {
        List<CategoryEntity> collect = categoryEntities.stream().filter(cat -> cat.getParentCid() == parentCid)
                .collect(Collectors.toList());
        return collect;
    }

    public List<Long> findParentPath(Long catalogId, List<Long> paths) {
        paths.add(catalogId);
        CategoryEntity byId = this.getById(catalogId);
        if (byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;
    }

}