package com.example.demo.config;

import com.example.demo.entity.LoginMessage;
import com.example.demo.entity.RegistrationMessage;
import com.example.demo.entity.TestData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

    public void sendTransactionalMessage(String msg) {
        // 是否开启事务判断
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    rabbitTemplate.convertAndSend("transactional", "", msg);
                }
            });
        } else {
            rabbitTemplate.convertAndSend("transactional", "", msg);
        }
    }

}
