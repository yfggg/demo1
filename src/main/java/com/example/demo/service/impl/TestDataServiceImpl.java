package com.example.demo.service.impl;

import com.example.demo.entity.TestData;
import com.example.demo.mapper.TestDataMapper;
import com.example.demo.service.ITestDataService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * <p>
 * 测试数据表 服务实现类
 * </p>
 *
 * @author yf
 * @since 2022-03-09
 */
@Service
public class TestDataServiceImpl extends ServiceImpl<TestDataMapper, TestData> implements ITestDataService {

    @Override
    public List<TestData> queryAll(Integer s) {
        return this.baseMapper.queryAll(s);
    }

    @Async("asyncServiceExecutor")
    @Override
    public CompletableFuture<List<TestData>> completableFutureTask(Integer s) {
        log.warn(Thread.currentThread().getName() + "start this task!");
        return CompletableFuture.completedFuture(this.baseMapper.queryAll(s));
    }

}
