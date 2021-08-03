package com.dzy.gulimall.seckill.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


/**
 * @EnableScheduling 注解开启定时任务
 * @Scheduled       注解开启一个定时任务
 */

@Slf4j
//@Component
//@EnableScheduling
//@EnableAsync
public class HelloSchedule {

    /**
     *  spring的cron表达式和quartz的基本一致
     *      1、但是spring中的cron表达式不支持年
     *      2、spring中的周 1表示的是周一，而quartz里面1表示周日
     *
     */
//    @Async
//    @Scheduled(cron = "* * * * * ?")
    public void hello() {
        log.info("hello...");
        try {
            TimeUnit.SECONDS.sleep(30);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
