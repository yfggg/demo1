package com.example.demo.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags="统计")
@Slf4j
@RestController
@RequestMapping("/statistics")
public class StatisticsController {
}
