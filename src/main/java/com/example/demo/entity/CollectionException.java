package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

import com.example.demo.enums.CollectionEnum;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

/**
 * <p>
 * 采集异常
 * </p>
 *
 * @author yf
 * @since 2022-02-15
 */
@Getter
@Setter
@TableName("collection_exception")
public class CollectionException implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文档id
     */
    @Id
    @TableId("doc_id")
    private String docId;

    /**
     * 批次id
     */
    private String batchId;

    /**
     * 文档索引
     */
    private String docIndex;

    /**
     * 类型
     */
    private CollectionEnum dataType;

    /**
     * 异常数量
     */
    private Integer exceptionCount;

    /**
     * 异常描述（用逗号分隔）
     */
    private String exceptionDescription;

    /**
     * 处理标志（0未处理 1已处理）
     */
    private String handleFlag;

    /**
     * 删除标志(0代表存在 1代表删除)
     */
    private String delFlag;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;


}
