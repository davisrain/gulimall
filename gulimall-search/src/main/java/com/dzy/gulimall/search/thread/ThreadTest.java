package com.dzy.gulimall.search.thread;

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
        System.out.println("main......start......");
        //执行方法完成后的处理
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
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10 / 0;
            System.out.println("运行结果是：" + i);
            return i;
        }, service).thenApplyAsync((res) -> {
            System.out.println("任务2启动了...");
            System.out.println("上一个任务的返回结果是：" + res);
            return res * 2;
        }, service);
        System.out.println("返回结果：" + future.get());
        System.out.println("main......end......");
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
