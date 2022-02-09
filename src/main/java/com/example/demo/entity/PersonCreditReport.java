package com.example.demo.entity;

import io.swagger.annotations.ApiModel;
import lombok.Data;

@ApiModel(value="采集对象", description="")
@Data
public class PersonCreditReport {

    private String identificationNumber;
    private String name;
    private String area;
    private Integer age;
    private String md5;

}
