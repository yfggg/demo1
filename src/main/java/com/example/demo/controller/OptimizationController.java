package com.example.demo.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.example.demo.aop.Timer;
import com.example.demo.config.MessagingService;
import com.example.demo.entity.Account;
import com.example.demo.entity.Result;
import com.example.demo.entity.TestData;
import com.example.demo.service.IAccountService;
import com.example.demo.service.ITestDataService;
import com.example.demo.utils.EsUtil;
import com.example.demo.utils.RedisUtil;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private IAccountService accountService;

    @Timer
    @GetMapping(value = "/insert")
    public String insert() {

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
        esUtil.createIndex("test");
        esUtil.bulkAddData("test", queryAll());

        /*4*/
        messagingService.sendInsertMessage(queryAll());

        return null;
    }

    public List<TestData> queryAll() {
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
        return all;
    }

    @Timer
    @GetMapping(value = "/thread")
    public void thread() {
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                System.out.println(Thread.currentThread().getName() + "--进入Runner1运行状态——————————" + i);
            }
        });
        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                System.out.println(Thread.currentThread().getName() + "--进入Runner2运行状态==========" + i);
            }
        });
        thread1.start();
        thread2.start();
//        thread1.run();
//        thread2.run();
    }

    @Timer
    @ApiOperation(value="并发插入测试")
    @PostMapping(value = "/kill")
    public void kill() throws InterruptedException {
        RLock rLock = redissonClient.getLock("test");
        // 尝试加锁，最多等待3秒，上锁以后10秒自动解锁
        if (rLock.tryLock(3, 10, TimeUnit.SECONDS)) {
            try {
                someting();
            } finally {
                rLock.unlock();
            }
        }
    }

    // 必须放在接口外面，那不然每个请求的lock是不同的！！！
    private final static Lock lock = new ReentrantLock();
    @Timer
    @ApiOperation(value="并发插入测试2")
    @PostMapping(value = "/kill2")
    public void kill2() throws InterruptedException {
        // 尝试加锁，最多等待3秒
        if (lock.tryLock(3, TimeUnit.SECONDS)) {
            try {
                someting();
            } finally {
                lock.unlock();
            }
        }
    }

    private void someting() {
        QueryWrapper<Account> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("amount", "yf");
        int count = accountService.count(queryWrapper);
        if(0 >= count) {
            Account account = new Account();
            account.setAmount("yf");
            accountService.save(account);
        }
    }


}

