package com.dzy.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.dzy.gulimall.product.service.CategoryBrandRelationService;
import com.dzy.gulimall.product.vo.Catalog2Vo;
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

import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzy.common.utils.PageUtils;
import com.dzy.common.utils.Query;

import com.dzy.gulimall.product.dao.CategoryDao;
import com.dzy.gulimall.product.entity.CategoryEntity;
import com.dzy.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedissonClient redisson;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listByTree() {
        //1、查出所有分类
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);
        //2、组装树形结构
        List<CategoryEntity> categories = categoryEntities.stream().filter(category -> category.getParentCid() == 0)
                .map(category -> {
                    category.setSubCategories(getSubCategories(category, categoryEntities));
                    return category;
                }).sorted((category1, category2) -> (category1.getSort() == null? 0 : category1.getSort()) - (category2.getSort() == null? 0: category2.getSort()))
                .collect(Collectors.toList());
        return categories;
    }

    @Override
    public int removeCategoriesByIds(List<Long> catIds) {
        //TODO 检查当前删除的分类，是否被别的地方引用
        return baseMapper.deleteBatchIds(catIds);
    }

    @Override
    public Long[] getCatelogPath(Long catelogId) {
        List<Long> list = new ArrayList<>();
        findCatelogPath(catelogId, list);
        Collections.reverse(list);
        return list.toArray(new Long[0]);
    }

    public void findCatelogPath(Long catelogId, List<Long> list) {
        list.add(catelogId);
        CategoryEntity category = this.getById(catelogId);
        if(category != null && category.getParentCid() != 0) {
            findCatelogPath(category.getParentCid(), list);
        }
    }


    @Override
    @Transactional
//    @CacheEvict(value = "category", key = "'getLevel1Categories'")
//    @Caching(evict = {
//            @CacheEvict(value = "category", key="'getLevel1Categories'"),
//            @CacheEvict(value = "category", key="'getCatalogJson'")
//    })
    @CacheEvict(value = "category", allEntries = true)
    public void updateDetail(CategoryEntity category) {
        updateById(category);
        if(StringUtils.hasText(category.getName())) {
            categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
            //TODO 更新其他关联
        }
    }


    //每一个缓存都需要指定将它放到哪个名字的缓存【缓存的分区】
    /* key,表示存入缓存中的key的名字，key可以使用spEL表达式，通过反射获取各种数据，如果要填入字符串，
        需要给字符串加单引号，表示不是表达式的形式 */
    //@Cacheable代表当前方法的结果需要缓存，如果缓存里面有，就去缓存中拿。如果缓存没有，就执行方法，并将返回结果放入缓存中
    @Cacheable(value = "category", key = "#root.methodName")
    @Override
    public List<CategoryEntity> getLevel1Categories() {
       return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
    }

    /**
     *  使用SpringCache实现的结合缓存的查询方法
     */
    @Override
    @Cacheable(value = "category", key = "#root.methodName")
    public Map<String, List<Catalog2Vo>> getCatalogJson() {
        List<CategoryEntity> categories = baseMapper.selectList(null);
        List<CategoryEntity> level1Categories = getCategoriesByParentCid(categories, 0L);
        Map<String, List<Catalog2Vo>> result = level1Categories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            List<CategoryEntity> level2Categories = getCategoriesByParentCid(categories, v.getCatId());
            return level2Categories.stream().map(level2Category -> {
                List<CategoryEntity> level3Categories = getCategoriesByParentCid(categories, level2Category.getCatId());
                List<Catalog2Vo.Catalog3Vo> catalog3Vos = level3Categories.stream().map(level3Category ->
                        new Catalog2Vo.Catalog3Vo(level2Category.getCatId().toString(), level3Category.getCatId().toString(), level3Category.getName())
                ).collect(Collectors.toList());
                return new Catalog2Vo(v.getCatId().toString(), level2Category.getCatId().toString(), level2Category.getName(), catalog3Vos);
            }).collect(Collectors.toList());
        }));
        return result;
    }

    /**
     *  使用redisTemplate和redisson实现的结合缓存查询方法
     */
    public Map<String, List<Catalog2Vo>> getCatalogJson2() {
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        String catalogJson = opsForValue.get("catalogJson");
        //如果缓存中没有，从数据库里取出
        if(!StringUtils.hasText(catalogJson)) {
            Map<String, List<Catalog2Vo>> catalogJsonFromDB = getCatalogJsonFromDBLockedByRedisson();
            return catalogJsonFromDB;
        }
        System.out.println("缓存命中，直接返回");
        return JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catalog2Vo>>>() {});
    }

    public Map<String, List<Catalog2Vo>> getCatalogJsonFromDBLockedByRedisson() {
        //锁的粒度越细，速度越快
        RLock lock = redisson.getLock("catalogJson-lock");
        lock.lock();
        try {
            return getCatalogJsonFromDB();
        } finally {
            lock.unlock();
        }
    }


    public Map<String, List<Catalog2Vo>> getCatalogJsonFromDBLockedByRedis() {
        //本地锁 synchronized JUC（lock） 等方法在分布式的情况无法完全锁住所有线程，只能锁住自己本服务的线程。使用分布式锁。
        //将本地锁替换为redis的分布式锁
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        String uuid = UUID.randomUUID().toString();
        Boolean lock = ops.setIfAbsent("lock", uuid, 30, TimeUnit.SECONDS);
        if (lock) {
            //stringRedisTemplate.expire("lock", 30, TimeUnit.SECONDS);
            System.out.println("获取分布式锁成功...");
            Map<String, List<Catalog2Vo>> catalogJson = null;
            try {
                catalogJson = getCatalogJsonFromDB();
            } finally {
                //获取lock值进行比较，相同说明是自己线程的锁，可以删除
                //使用lua脚本来保证获取值和删除操作的原子性
                String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1] then return redis.call(\"del\",KEYS[1]) else return 0 end";
                Long result = stringRedisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
                        Collections.singletonList("lock"), uuid);
            }
            return catalogJson;
        } else {
            //休眠2000毫秒，为了防止超过栈帧深度造成StackOverFlow
            System.out.println("获取分布式锁失败，正在重试...");
            try {
                TimeUnit.MILLISECONDS.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //自旋进行等待
            return getCatalogJsonFromDBLockedByRedis();
        }
    }

    private Map<String, List<Catalog2Vo>> getCatalogJsonFromDB() {
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        String catalogJson = opsForValue.get("catalogJson");
        if(StringUtils.hasText(catalogJson)) {
            System.out.println("缓存命中，直接返回");
            return JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catalog2Vo>>>(){});
        }
        System.out.println("缓存不命中，查询了数据库");
        //对业务逻辑进行优化，避免循环取
        //将我们需要的分类数据一次性从库里全部取出，再进行遍历循环筛选
        List<CategoryEntity> categories = baseMapper.selectList(null);
        List<CategoryEntity> level1Categories = getCategoriesByParentCid(categories, 0L);
        Map<String, List<Catalog2Vo>> result = level1Categories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            List<CategoryEntity> level2Categories = getCategoriesByParentCid(categories, v.getCatId());
            return level2Categories.stream().map(level2Category -> {
                List<CategoryEntity> level3Categories = getCategoriesByParentCid(categories, level2Category.getCatId());
                List<Catalog2Vo.Catalog3Vo> catalog3Vos = level3Categories.stream().map(level3Category ->
                        new Catalog2Vo.Catalog3Vo(level2Category.getCatId().toString(), level3Category.getCatId().toString(), level3Category.getName())
                ).collect(Collectors.toList());
                return new Catalog2Vo(v.getCatId().toString(), level2Category.getCatId().toString(), level2Category.getName(), catalog3Vos);
            }).collect(Collectors.toList());
        }));
        //将取出的数据放入缓存，方便下次调用
        opsForValue.set("catalogJson", JSON.toJSONString(result));
        return result;
    }

    private List<CategoryEntity> getCategoriesByParentCid(List<CategoryEntity> categories, Long parentCid) {
        return categories.stream().filter(category -> category.getParentCid().equals(parentCid)).collect(Collectors.toList());
    }



    private List<CategoryEntity> getSubCategories(CategoryEntity root, List<CategoryEntity> all) {
        return all.stream().filter(category -> category.getParentCid().equals(root.getCatId()))
                .map(category -> {
                    category.setSubCategories(getSubCategories(category, all));
                    return category;
                }).sorted((category1, category2) -> (category1.getSort() == null? 0 : category1.getSort()) - (category2.getSort() == null? 0: category2.getSort()))
                .collect(Collectors.toList());
    }


}