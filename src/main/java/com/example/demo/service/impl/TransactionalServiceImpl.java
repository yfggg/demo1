package com.example.demo.service.impl;

import com.example.demo.config.MessagingService;
import com.example.demo.entity.Account;
import com.example.demo.entity.RegistrationMessage;
import com.example.demo.entity.TestData;
import com.example.demo.service.IAccountService;
import com.example.demo.service.ITestDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author yang fan
 * @since 2021-12-13
 */
@Service
public class TransactionalServiceImpl{

    @Autowired
    private ITestDataService testDataService;

    @Autowired
    private IAccountService accountService;

    @Autowired
    private MessagingService messagingService;

    @Transactional(rollbackFor = RuntimeException.class)
    public void test() {
        Account account = new Account();
        account.setName("zzz");
        accountService.save(account);
        TestData testData = new TestData();
        testData.setName("yyy");
        testDataService.save(testData);
        messagingService.sendTransactionalMessage("事务队列运行了！！！");
        //人为制造一个错误
        System.out.println(1/0);
    }

}
