package com.example.demo.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.Account;
import com.example.demo.entity.War;
import com.example.demo.mapper.AccountMapper;
import com.example.demo.service.IAccountService;
import com.example.demo.service.IWarService;
import com.example.demo.vo.AccountVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author yang fan
 * @since 2021-12-13
 */
@Service
public class AccountServiceImpl extends ServiceImpl<AccountMapper, Account> implements IAccountService {

    @Autowired
    private IWarService warService;

    @Override
    public IPage<Account> selectPage(Page page, Wrapper<AccountVO> queryWrapper) {
        return this.baseMapper.selectPageList(page, queryWrapper);
    }

    @Override
    @Transactional
    public boolean multiTablesSave(Account account) {
        boolean result = false;
        // TODO
        // 判断数据库中是否存在
        // 如果不存在就插入
//        result = this.saveOrUpdate();
//        if(result && StrUtil.isNotBlank(account.getWeapon())) {
//            War war = new War();
//            war.setAccountId(account.getId());
//            war.setWeapon(account.getWeapon());
////            QueryWrapper<War> queryWrapper = new QueryWrapper<>();
////            warService.getOne(queryWrapper.eq("is_deleted", "0"));
//            try {
//                result = warService.save(war);
//            } catch (Exception e) {
//                throw new RuntimeException();
//            }
//        }
        return result;
    }

}
