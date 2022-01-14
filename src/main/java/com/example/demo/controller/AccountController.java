package com.example.demo.controller;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.example.demo.aop.IsRepeatSubmit;
import com.example.demo.aop.Timer;
import com.example.demo.entity.Account;
import com.example.demo.entity.Result;
import com.example.demo.service.IAccountService;
import com.example.demo.enums.ColorEnum;
import com.example.demo.vo.AccountVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;


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
    private IAccountService accountService;

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
    @ApiOperation(value="分页查询")
    @GetMapping(value = "/queryPageList")
    public Result<?> queryPageList(AccountVO vo,
                                   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
                                   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize) {
        QueryWrapper<AccountVO> queryWrapper = new QueryWrapper<>();

        // 精确查询
        queryWrapper.and(
                ObjectUtils.isNotNull(vo.getStatus()),
                wrapper -> wrapper.eq("status", vo.getStatus())
        );

        // 多条件模糊查询 不走索引
        if(StrUtil.isNotBlank(vo.getName()) || StrUtil.isNotBlank(vo.getAmount())) {
            queryWrapper.and(
                    wrapper -> wrapper.like("name", vo.getName())
                            .or()
                            .like("amount", vo.getAmount())
            );
        }

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

        // 排序 递减
        queryWrapper.orderByDesc("create_time");

        // 分页 需要配置插件
        Page<Account> page = new Page<>(pageNo, pageSize);
        IPage<Account> pageList = accountService.selectPage(page, queryWrapper);

        return Result.OK(pageList);
    }

    @IsRepeatSubmit(intervalTime=5, msg="禁止重复提交")
    @Timer
    @ApiOperation(value="插入")
    @PostMapping(value = "/save")
    public Result<?> save(@RequestBody Account account) {
        return Result.OK(accountService.save(account));
    }

    @Timer
    @ApiOperation(value="批量插入")
    @PostMapping(value = "/saveBatch")
    public Result<?> saveBatch(@RequestBody List<Account> accounts) {
        return Result.OK(accountService.saveBatch(accounts));
    }

}

