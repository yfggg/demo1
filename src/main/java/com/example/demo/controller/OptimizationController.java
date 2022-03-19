package com.example.demo.controller;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.example.demo.aop.Timer;
import com.example.demo.config.MessagingService;
import com.example.demo.entity.TestData;
import com.example.demo.service.IAccountService;
import com.example.demo.service.ITestDataService;
import com.example.demo.utils.EsUtil;
import com.example.demo.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

/**
 * <p>
 * 测试数据表 前端控制器
 * </p>
 *
 * @author yf
 * @since 2022-03-09
 */
@Slf4j
@RestController
@RequestMapping("/test-data")
public class OptimizationController {

    @Autowired
    private ITestDataService testDataService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private EsUtil esUtil;

    @Autowired
    private MessagingService messagingService;

    @Timer
    @GetMapping(value = "/test")
    public String test() throws ExecutionException, InterruptedException {

        /*1*/
//        List<TestData> list = testDataService.list();
//        redisUtil.set("test_data", list);

        /*2*/
//        List<TestData> all = new ArrayList<>();
//        for (int i = 1; i <= 100; i++) {
//            int startIdx = i * 10000;
//            all.addAll(testDataService.queryAll(startIdx));
//        }
//        redisUtil.set("test_data", all);

        /*3*/
//        List<CompletableFuture<List<TestData>>> futureList = new ArrayList<>();
//        for (int i = 1; i <= 100; i++) {
//            int startIdx = i * 10000;
//            CompletableFuture<List<TestData>> completableFuture = testDataService.completableFutureTask(startIdx);
//            futureList.add(completableFuture);
//        }
//        List<TestData> all = new ArrayList<>();
//        futureList.stream().forEach(future -> {
//            try {
//                all.addAll(future.get());
//            } catch (InterruptedException | ExecutionException e) {
//                log.error("多线程查询出现异常：{}", e.getMessage());
//            }
//        });
//        esUtil.createIndex("test");
//        esUtil.bulkAddData("test", all);

        /*4*/
        List<CompletableFuture<List<TestData>>> futureList = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            int startIdx = i * 10000;
            CompletableFuture<List<TestData>> completableFuture = testDataService.completableFutureTask(startIdx);
            futureList.add(completableFuture);
        }
        List<TestData> all = new ArrayList<>();
        futureList.stream().forEach(future -> {
            try {
                all.addAll(future.get());
            } catch (InterruptedException | ExecutionException e) {
                log.error("多线程查询出现异常：{}", e.getMessage());
            }
        });
        messagingService.sendInsertMessage(all);

        return null;
    }

    @Timer
    @GetMapping(value = "/thread")
    public void thread() {
        Runner1 runner1 = new Runner1();
        Runner2 runner2 = new Runner2();
//        Thread(Runnable target) 分配新的 Thread 对象。
        Thread thread1 = new Thread(runner1);
        Thread thread2 = new Thread(runner2);
        thread1.start();
        thread2.start();
//        thread1.run();
//        thread2.run();
    }

    class Runner1 implements Runnable { // 实现了Runnable，jdk就知道这个类是一个线程
        public void run() {
            for (int i = 0; i < 100000; i++) {
                System.out.println(Thread.currentThread().getName() + "--进入Runner1运行状态——————————" + i);
            }
        }
    }

    class Runner2 implements Runnable { // 实现了Runnable，jdk就知道这个类是一个线程
        public void run() {
            for (int i = 0; i < 100000; i++) {
                System.out.println(Thread.currentThread().getName() + "--进入Runner2运行状态==========" + i);
            }
        }
    }

}

