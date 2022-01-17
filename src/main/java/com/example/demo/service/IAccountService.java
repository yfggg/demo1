package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.entity.Account;
import com.example.demo.vo.AccountVO;


/**
 * <p>
 *  服务类
 * </p>
 *
 * @author yang fan
 * @since 2021-12-13
 */
public interface IAccountService extends IService<Account> {

    IPage<Account> selectPage(Page page, Wrapper<AccountVO> queryWrapper);

    boolean multiTablesSave(Account account);
}
