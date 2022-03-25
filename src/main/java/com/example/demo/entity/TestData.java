package com.example.demo.entity;

import cn.afterturn.easypoi.excel.annotation.Excel;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * <p>
 * 测试数据表
 * </p>
 *
 * @author yf
 * @since 2022-03-09
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@TableName("test_data")
public class TestData implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 名称
     */
    @Excel(name = "名称")
    private String name;

    /**
     * 密码
     */
    @Excel(name = "密码")
    private String pwd;

    /**
     * 创建日期
     */
    @Excel(name = "创建日期", width = 20, format = "yyyy-mm-dd")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;


}
