package com.dzy.gulimall.search.thread;

import java.sql.Time;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadTest {

    public static ExecutorService service = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        /**
         *  多任务组合
         */
        System.out.println("main.....start......");
        CompletableFuture<String> futureImg = CompletableFuture.supplyAsync(() -> {
            System.out.println("图片加载完成");
            return "hello.jpg";
        }, service);
        CompletableFuture<String> futureAttr = CompletableFuture.supplyAsync(() -> {
            System.out.println("属性加载完成");
            return "黑色256GB";
        }, service);
        CompletableFuture<String> futureTitle = CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(3);
                System.out.println("标题加载完成");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "华为";
        }, service);
        CompletableFuture<Object> future = CompletableFuture.anyOf(futureImg, futureAttr, futureTitle);
        System.out.println("其中一个任务的返回结果：" + future.get());
        System.out.println("main.....end......");

        /**
         *  两个任务组合，一个完成
         */
//        System.out.println("main.....start......");
//        CompletableFuture<Integer> future01 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("任务1线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("任务1结果是：" + i);
//            return i;
//        }, service);
//        CompletableFuture<Integer> future02 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("任务2线程：" + Thread.currentThread().getId());
//            int i = 0;
//            try {
//                TimeUnit.SECONDS.sleep(3);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            System.out.println("任务2结果是：" + i);
//            return i;
//        }, service);
//        CompletableFuture<String> future = future01.applyToEitherAsync(future02, (res) -> {
//            System.out.println("任务3启动....线程为：" + Thread.currentThread().getId());
//            System.out.println("拿到其中一个完成任务的返回值为：" + res);
//            return "task3 complete";
//        }, service);
//        System.out.println("任务3返回结果：" + future.get());
//        System.out.println("main.....end......");


        /**
         *  两个任务组合，都要完成
         */
//        System.out.println("main.....start......");
//        CompletableFuture<Integer> future01 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("任务1线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("任务1结果是：" + i);
//            return i;
//        }, service);
//        CompletableFuture<String> future02 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("任务2线程：" + Thread.currentThread().getId());
//            String s = "hello";
//            System.out.println("任务2结果是：" + s);
//            return s;
//        }, service);
//        CompletableFuture<String> future = future01.thenCombineAsync(future02, (f1, f2) -> {
//            System.out.println("任务3启动....线程为：" + Thread.currentThread().getId());
//            System.out.println("任务1的返回结果：" + f1 + ", 任务2的返回结果：" + f2);
//            return f2 + " World!" + f1;
//        }, service);
//        System.out.println("任务3的返回结果：" + future.get() );
//        System.out.println("main.....end......");


        /**
         * 执行方法完成后的处理
         */
//        System.out.println("main......start......");
//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 0;
//            System.out.println("运行结果是：" + i);
//            return i;
//        }, service).handleAsync((res, exception) -> {
//            System.out.println("异常是：" + exception);
//            return 10;
//        }, service);
//        System.out.println("返回结果：" + future.get());
        /**
         * 异步任务串行化
         */
//        System.out.println("main......start......");
//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 0;
//            System.out.println("运行结果是：" + i);
//            return i;
//        }, service).thenApplyAsync((res) -> {
//            System.out.println("任务2启动了...");
//            System.out.println("上一个任务的返回结果是：" + res);
//            return res * 2;
//        }, service);
//        System.out.println("返回结果：" + future.get());
//        System.out.println("main......end......");
    }


    public static void thread(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main......start......");
//        new Thread01().start();
//        new Thread(new Runnable01()).start();
//        FutureTask<Integer> futureTask = new FutureTask<>(new Callable01());
//        new Thread(futureTask).start();
//        //get方法会进行阻塞等待线程执行完成拿到返回值
//        Integer i = futureTask.get();
//        System.out.println("线程执行返回结果为：" + i);
//        ExecutorService service = Executors.newFixedThreadPool(10);
//        //没有返回值
//        service.execute(new Runnable01());
//        //有返回值
//        Future<Integer> future = service.submit(new Callable01());
//        Integer i = future.get();
//        System.out.println("线程池执行返回值为：" + i);
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(5,
                100,
                10,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
        System.out.println("main......end......");
    }

    public static class Callable01 implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10/2;
            System.out.println("运行结果是：" + i);
            return i;
        }
    }

    public static class Runnable01 implements Runnable {
        @Override
        public void run() {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10/2;
            System.out.println("运行结果是：" + i);
        }
    }

    public static class Thread01 extends Thread {
        @Override
        public void run() {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10/2;
            System.out.println("运行结果是：" + i);
        }
    }


}
