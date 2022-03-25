package com.example.demo.controller;

import com.example.demo.aop.Timer;
import com.example.demo.service.impl.TransactionalServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Api(tags="事务")
@Slf4j
@RestController
@RequestMapping("/transactional")
public class TransactionalController {

    @Autowired
    TransactionalServiceImpl transactionalService;

    @Timer
    @ApiOperation(value="test")
    @PostMapping(value = "/test")
    public void test() {
        transactionalService.test();
    }

}
