package com.example.demo.controller;

import com.example.demo.utils.RedisUtils;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Api(tags="缓存")
@Slf4j
@RestController
@RequestMapping("/redis")
public class RedisController {

    @Resource
    private RedisUtils redisUtils;

    /**
     * 插入缓存数据
     */
    @PostMapping(value = "/set")
    public void set() {
//        redisUtils.set("yfyffff", "abv", 20, TimeUnit.SECONDS);
    }

    /**
     * 读取缓存数据
     */
    @GetMapping(value = "/get")
    public void get() {
        System.out.println(redisUtils.get("yf_key"));
//        System.out.println(redisUtils.containsKey("123"));
    }

}
