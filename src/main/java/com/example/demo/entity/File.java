package com.example.demo.entity;

import cn.afterturn.easypoi.excel.annotation.Excel;
import lombok.Data;

@Data
public class File {

    @Excel(name = "姓名", width = 15)
    private String name;

    @Excel(name = "身份证号", width = 15)
    private String idCard;
}
