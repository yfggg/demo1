package com.example.demo.controller;

import com.example.demo.aop.Timer;
import com.example.demo.entity.File;
import com.example.demo.entity.TestData;
import com.example.demo.service.ITestDataService;
import com.example.demo.utils.FileUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Api(tags="excel文档")
@Slf4j
@RestController
@RequestMapping("/excel")
public class ExcelController {

    @Autowired
    private ITestDataService testDataService;

    @Autowired
    OptimizationController optimizationController;

    @Timer
    @ApiOperation(value = "大数据量下载excel")
    @PostMapping(value = "/excel")
    public String excel(HttpServletResponse response) {
        List<TestData> all = optimizationController.queryAll();
        FileUtil.writeBigExcel("测试", all, TestData.class, response);
        return null;
    }


}