package com.example.demo.controller;

import com.example.demo.aop.Timer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags="统计")
@Slf4j
@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    @Timer
    @ApiOperation(value="自然人信用报告上链总数(按年龄层统计)")
    @PostMapping(value = "/test")
    public String test() {

        return null;
    }
}
