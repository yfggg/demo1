package com.example.demo.controller;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.example.demo.aop.IsRepeatSubmit;
import com.example.demo.aop.Timer;
import com.example.demo.entity.Account;
import com.example.demo.entity.CollectionException;
import com.example.demo.entity.Result;
import com.example.demo.entity.War;
import com.example.demo.enums.CollectionEnum;
import com.example.demo.service.IAccountService;
import com.example.demo.service.ICollectionExceptionService;
import com.example.demo.service.IWarService;
import com.example.demo.utils.EsUtil;
import com.example.demo.utils.ObjFieldsMapper;
import com.example.demo.vo.AccountVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author yang fan
 * @since 2021-12-13
 */
@Api(tags="account")
@Slf4j
@RestController
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private EsUtil esUtil;

    @Autowired
    private IAccountService accountService;

    @Autowired
    private ICollectionExceptionService collectionExceptionService;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 打印sql 生产环境不建议使用
     * 状态码 通过枚举展示中文
     *
     * @param vo
     * @param pageNo
     * @param pageSize
     * @return
     */
    @Timer
    @ApiOperation(value="多表关联分页查询")
    @GetMapping(value = "/queryPageList")
    public Result<?> queryPageList(AccountVO vo,
                                   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
                                   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize) {

        QueryWrapper<AccountVO> queryWrapper = new QueryWrapper<>();

        // 多条件模糊查询 不走索引
        queryWrapper.and(
                wrapper -> {
                    if(StrUtil.isNotBlank(vo.getName())) {
                        wrapper.like("name", vo.getName());
                    }
                    wrapper.or();
                    if(StrUtil.isNotBlank(vo.getAmount())) {
                        wrapper.like("amount", vo.getAmount());
                    }
                    wrapper.or();
                    if(StrUtil.isNotBlank(vo.getWeapon())) {
                        wrapper.like("war.weapon", vo.getWeapon());
                    }
                }
        );

        // 精确查询
        queryWrapper.and(
                StrUtil.isNotBlank(vo.getStatus()),
                wrapper -> wrapper.eq("status", vo.getStatus())
        );

        // 时间段查询
        queryWrapper.and(
                StrUtil.isNotBlank(vo.getStartTime()),
                wrapper -> wrapper.ge("create_time", vo.getStartTime())
        );
        if(StrUtil.isNotBlank(vo.getEndTime())) {
            LocalDateTime parseEndTime = DateUtil.parseLocalDateTime(vo.getEndTime(),"yyyy-MM-dd").plusDays(1L);
            String formatEndTime = LocalDateTimeUtil.format(parseEndTime, DatePattern.NORM_DATE_PATTERN);
            queryWrapper.and(wrapper -> wrapper.le("create_time", formatEndTime));
        }

        // 是否已经删除
        queryWrapper.eq("is_deleted", "0");

        // 排序 递减
        queryWrapper.orderByDesc("create_time");

        // 分页 需要配置插件
        Page<Account> page = new Page<>(pageNo, pageSize);
        IPage<Account> pageList = accountService.selectPage(page, queryWrapper);

        return Result.OK(pageList);
    }

     @Timer
     @ApiOperation(value="并发插入测试")
     @PostMapping(value = "/test")
     public Result<?> test() throws InterruptedException {
         RLock rLock = redissonClient.getLock("test");
         // 尝试加锁，最多等待3秒，上锁以后10秒自动解锁
         boolean res = rLock.tryLock(3, 10, TimeUnit.SECONDS);
         if (res) {
             QueryWrapper<Account> queryWrapper = new QueryWrapper<>();
             queryWrapper.eq("amount", "yf");
             int count = accountService.count(queryWrapper);
             if(0 >= count) {
                 Account account = new Account();
                 account.setAmount("yf");
                 accountService.save(account);
             }
             rLock.unlock();
         }
         return Result.OK();
    }

}

