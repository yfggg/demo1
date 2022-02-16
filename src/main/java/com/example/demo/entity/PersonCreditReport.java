package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import lombok.Data;

@ApiModel(value="个人信用报告", description="")
@Data
public class PersonCreditReport {

    private String id;

    // 批次id
    private String batchId;

    // 姓名
    private String name;

    // 身份证号码
    private String identificationNumber;

    // 区域
    private String area;

    // 年龄
    private Integer age;

    // 户口所在地
    private String accountLocation;

    // 茉莉分
    private Double mollyScore;

    // 信用等级
    private String creditLevel;

    // 二维码
    private String url;

    // 对_source加密
    private String md5;

    // 合规性标记
    private String filter;

    // 公示日期
    private String releaseDate;

    // 上链时间(上链用)
    private String slsj;
    // 交易id(上链用)
    private String txid;
    // 信息(上链用)
    private String info;
    // 账本(上链用 现在好像不需要了)
    private String ledger;
}
