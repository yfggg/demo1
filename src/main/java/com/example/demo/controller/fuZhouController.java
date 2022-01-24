package com.example.demo.controller;

import com.example.demo.entity.English;
import com.example.demo.entity.Pinyin;
import com.example.demo.service.impl.FuZhouServiceImpl;
import com.example.demo.utils.ObjFieldsMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags="福州项目")
@Slf4j
@RestController
@RequestMapping("/fuZhou")
public class fuZhouController {

    @Autowired
    FuZhouServiceImpl fuZhouService;

    @ApiOperation(value="测试")
    @GetMapping(value = "/test")
    public void test() {
        //log.info(fuZhouService.pinyinToEnglish());
    }

}
