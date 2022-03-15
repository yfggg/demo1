package com.example.demo.service;

import com.example.demo.entity.TestData;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * 测试数据表 服务类
 * </p>
 *
 * @author yf
 * @since 2022-03-09
 */
public interface ITestDataService extends IService<TestData> {

    List<TestData> queryAll(Integer s);

    CompletableFuture<List<TestData>> completableFutureTask(Integer s);
}
