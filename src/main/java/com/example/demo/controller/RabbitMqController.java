package com.example.demo.controller;

import com.example.demo.aop.Timer;
import com.example.demo.config.MessagingService;
import com.example.demo.entity.LoginMessage;
import com.example.demo.entity.RegistrationMessage;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags="rabbitMq")
@Slf4j
@RestController
@RequestMapping("/rabbitMq")
public class RabbitMqController {

    @Autowired
    private MessagingService messagingService;

    @Timer
    @PostMapping(value = "/register")
    public void register() {
        messagingService.sendRegistrationMessage(new RegistrationMessage("yf", "497302152@qq.com"));
    }

    @Timer
    @PostMapping(value = "/login-fail")
    public void fail() {
        messagingService.sendLoginMessage(new LoginMessage("wyp", "478240607@qq.com", false));
    }

    @Timer
    @PostMapping(value = "/login-succes")
    public void succes() {
        messagingService.sendLoginMessage(new LoginMessage("yf", "497302152@qq.com", true));
    }

}
