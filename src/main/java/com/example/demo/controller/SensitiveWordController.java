package com.example.demo.controller;

import cn.hutool.dfa.WordTree;
import com.example.demo.aop.Timer;
import com.example.demo.entity.Bucket;
import com.example.demo.utils.SensitiveWordUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags="敏感词")
@Slf4j
@RestController
@RequestMapping("/sensitive")
public class SensitiveWordController {

    @Timer
    @ApiOperation(value="测试")
    @PostMapping(value = "/test")
    public String test() {
        //正文
        String text = "我有一颗大**土豆，刚出锅的刚出锅的";

        List<String> matchAll = SensitiveWordUtil.matchAll(text,
                "小, 土豆, 刚出锅",
                false, false);

        if(matchAll.size() > 0) {
            log.error(matchAll.toString());
        }

        return null;
    }
}
