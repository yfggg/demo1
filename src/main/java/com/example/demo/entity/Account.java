package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.demo.enums.ColorEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.NonNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author yang fan
 * @since 2021-12-13
 */
@ApiModel(value="Account对象", description="")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Account implements Serializable {

    private static final long serialVersionUID=1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    private String name;

    private String amount;

    private ColorEnum status;

    @TableField(fill = FieldFill.INSERT)
    private String isDeleted;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updataTime;

    /**********************************
     数据库表中不存在以下字段(表join时会用到)
     **********************************/

    @TableField(exist = false)
    private String weapon;
}
