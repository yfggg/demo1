package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.Account;
import com.example.demo.entity.TestData;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 测试数据表 Mapper 接口
 * </p>
 *
 * @author yf
 * @since 2022-03-09
 */
@Mapper
public interface TestDataMapper extends BaseMapper<TestData> {

    @Select("<script> SELECT `id`,`name`,`pwd` FROM `test_data` where id > #{s} limit 10000 </script>")
    List<TestData> queryAll(@Param("s") Integer s);
}
