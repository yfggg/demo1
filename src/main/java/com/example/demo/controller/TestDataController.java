package com.example.demo.controller;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.example.demo.aop.Timer;
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
public class TestDataController {

    @Autowired
    private ITestDataService testDataService;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    EsUtil esUtil;

    @Timer
    @GetMapping(value = "/test")
    public String test() throws ExecutionException, InterruptedException {

//        List<TestData> all = new ArrayList<>();
//        for (int i = 1; i <= 100; i++) {
//            int startIdx = i * 10000;
//            all.addAll(testDataService.queryAll(startIdx));
//        }
//        redisUtil.set("test_data", all);


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
        esUtil.createIndex("test");
        esUtil.bulkAddData("test", all);
//        all.stream().forEach(data -> esUtil.addData("test", data));



//        List<TestData> list = testDataService.list();
//        redisUtil.set("test_data", list);

        return null;
    }

}

