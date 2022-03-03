package com.example.demo.controller;

import com.example.demo.aop.Timer;
import com.example.demo.entity.File;
import com.example.demo.utils.FileUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Api(tags="excel文档")
@Slf4j
@RestController
@RequestMapping("/excel")
public class ExcelController {

    @Timer
    @ApiOperation(value = "测试")
    @PostMapping(value = "/test")
    public String test(HttpServletResponse response) {
        // 读取
        List<File> list = FileUtil.readExcel("C:\\Users\\yf\\Desktop\\excel.xlsx", File.class);
        // 写入
        FileUtil.writeExcel("zzz", list, File.class, response);
        return null;
    }




}