package com.example.demo.config;

import com.example.demo.entity.LoginMessage;
import com.example.demo.entity.RegistrationMessage;
import com.example.demo.entity.TestData;
import com.example.demo.utils.EsUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class QueueMessageListener {

    @Autowired
    private EsUtil esUtil;

    static final String QUEUE_MAIL = "q_mail";
    static final String QUEUE_SMS = "q_sms";
    static final String QUEUE_APP = "q_app";
    static final String QUEUE_ES = "q_es";
    static final String QUEUE_TRANSACTIONAL = "q_transactional";

    @RabbitListener(queues = QUEUE_TRANSACTIONAL)
    public void onTransactionalMessageFromTransactionalQueue(String msg) {
        log.info(msg);
    }

    @RabbitListener(queues = QUEUE_ES)
    public void onInsertMessageFromEsQueue(List<TestData> all) {
        esUtil.createIndex("test");
        esUtil.bulkAddData("test", all);
        log.info("insert es success!");
    }

    @RabbitListener(queues = QUEUE_MAIL)
    public void onRegistrationMessageFromMailQueue(RegistrationMessage message) {
        log.info("queue {} received registration message: {}", QUEUE_MAIL, message);
    }

    @RabbitListener(queues = QUEUE_SMS)
    public void onRegistrationMessageFromSmsQueue(RegistrationMessage message) {
        log.info("queue {} received registration message: {}", QUEUE_SMS, message);
    }

    @RabbitListener(queues = QUEUE_MAIL)
    public void onLoginMessageFromMailQueue(LoginMessage message) {
        log.info("queue {} received login message: {}", QUEUE_MAIL, message);
    }

    @RabbitListener(queues = QUEUE_SMS)
    public void onLoginMessageFromSmsQueue(LoginMessage message) {
        log.info("queue {} received login message: {}", QUEUE_SMS, message);
    }

    @RabbitListener(queues = QUEUE_APP)
    public void onLoginMessageFromAppQueue(LoginMessage message) {
        log.info("queue {} received login message: {}", QUEUE_APP, message);
    }

}
