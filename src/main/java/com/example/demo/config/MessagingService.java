package com.example.demo.config;

import com.example.demo.entity.LoginMessage;
import com.example.demo.entity.RegistrationMessage;
import com.example.demo.entity.TestData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MessagingService {

    @Autowired
    RabbitTemplate rabbitTemplate;

    public void sendRegistrationMessage(RegistrationMessage msg) {
        rabbitTemplate.convertAndSend("registration", "", msg);
    }

    public void sendLoginMessage(LoginMessage msg) {
        String routingKey = msg.success ? "" : "login_failed";
        rabbitTemplate.convertAndSend("login", routingKey, msg);
    }

    public void sendInsertMessage(List<TestData> all) {
        rabbitTemplate.convertAndSend("insert", "", all);
    }

}
