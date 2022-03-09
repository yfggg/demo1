package com.example.demo.controller;

import com.example.demo.aop.Timer;
import com.example.demo.utils.FileUtil;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags="异步")
@Slf4j
@RestController
@RequestMapping("/async")
public class AsyncController {

//    @Resource
//    AsyncService asyncService;

//    @Autowired
//    FileUtil fileUtil;
//
//    @Timer
//    @PostMapping(value = "/test")
//    public void test() {
//        fileUtil.progressBar("bar", 1000L, total -> doSometing(total));
//    }
//
//    private void doSometing(Long total) {
//        try {
//            Thread.sleep(total);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }

    
}
