package com.dzy.gulimall.product.web;


import com.dzy.gulimall.product.entity.CategoryEntity;
import com.dzy.gulimall.product.service.CategoryService;
import com.dzy.gulimall.product.vo.Catalog2Vo;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
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
    CategoryService categoryService;

    @Autowired
    RedissonClient redisson;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @GetMapping({"/","/index.html"})
    public String indexPage(Model model) {

        List<CategoryEntity> categories = categoryService.getLevel1Categories();
        model.addAttribute("categories", categories);
        return "index";
    }

    @ResponseBody
    @GetMapping("/index/catalog.json")
    public Map<String, List<Catalog2Vo>> getCatelogJson() {
       return categoryService.getCatalogJson();
    }

    @ResponseBody
    @GetMapping("/hello")
    public String hello() {
        //1.获取锁，只要锁的名字一样，就是同一把锁
        RLock lock = redisson.getLock("my-lock");
        //2.加锁
//        lock.lock();    //阻塞式等待
        lock.lock(10, TimeUnit.SECONDS);    //设置10秒锁过期，自定义了锁过期时间之后锁不会自动续期。
        try {
            System.out.println("加锁成功，执行业务... " + Thread.currentThread().getId());
            TimeUnit.SECONDS.sleep(30);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //3.解锁
            System.out.println("释放锁... " + Thread.currentThread().getId());
            lock.unlock();
        }
        return "hello";
    }


    @GetMapping("/write")
    @ResponseBody
    public String writeValue() {
        RReadWriteLock lock = redisson.getReadWriteLock("rw-lock");
        RLock rLock = lock.writeLock();
        rLock.lock();
        try {
            System.out.println("获取写锁...");
            stringRedisTemplate.opsForValue().set("writeValue", UUID.randomUUID().toString());
            TimeUnit.SECONDS.sleep(10);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("释放写锁...");
            rLock.unlock();
        }
        return "ok";
    }

    @GetMapping("/read")
    @ResponseBody
    public String readValue() {
        RReadWriteLock lock = redisson.getReadWriteLock("rw-lock");
        RLock rLock = lock.readLock();
        rLock.lock();
        String writeValue = null;
        try {
            System.out.println("获取读锁...");
            writeValue = stringRedisTemplate.opsForValue().get("writeValue");
//            TimeUnit.SECONDS.sleep(10);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("释放读锁...");
            rLock.unlock();
        }
        return writeValue;
    }

    /**
     * 模拟停车逻辑
     */
    @GetMapping("/park")
    @ResponseBody
    public String park() throws InterruptedException {
        RSemaphore park = redisson.getSemaphore("park");
//        park.acquire(); //获取一个信号，获取一个值，占一个车位
        boolean b = park.tryAcquire();
        if(b) {
            //执行业务逻辑
            return "ok";
        } else {
            return "error";
        }
    }

    @GetMapping("/go")
    @ResponseBody
    public String go() {
        RSemaphore park = redisson.getSemaphore("park");
        park.release(); //释放一个信号，释放一个车位
        return "ok";
    }

    /**
     *  模拟放假锁门
     *  等到5个班的人都走完了，门卫才能锁门
     */
    @GetMapping("/lockDoor")
    @ResponseBody
    public String lockDoor() throws InterruptedException {
        RCountDownLatch countDownLatch = redisson.getCountDownLatch("lock-door");
        countDownLatch.trySetCount(5);  //设置倒计时为5
        countDownLatch.await();         //阻塞等待
        return "锁门完成...";
    }

    @GetMapping("/leave/{classId}")
    @ResponseBody
    public String leave(@PathVariable("classId") Long classId) {
        RCountDownLatch countDownLatch = redisson.getCountDownLatch("lock-door");
        countDownLatch.countDown();     //倒计时减1
        return classId + "班走完了...";
    }
}
