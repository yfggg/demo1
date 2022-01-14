package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author yang fan
 * @since 2021-12-13
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {

    @Select("SELECT * FROM `account` ${ew.customSqlSegment}")
    IPage<Account> selectPageList(Page page, @Param(Constants.WRAPPER) Wrapper wrapper);
}
