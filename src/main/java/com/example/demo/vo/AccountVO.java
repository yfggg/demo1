package com.example.demo.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 
 * </p>
 *
 * @author yang fan
 * @since 2021-12-13
 */
@Data
@ApiModel(value="Account对象", description="")
public class AccountVO {

    private String id;

    @ApiModelProperty(value ="姓名")
    private String name;

    @ApiModelProperty(value ="金额")
    private String amount;

    @ApiModelProperty(value ="开始时间")
    private String startTime;

    @ApiModelProperty(value ="结束时间")
    private String endTime;

    @ApiModelProperty(value ="状态")
    private String status;

    @ApiModelProperty(value ="兵器")
    private String weapon;
}
