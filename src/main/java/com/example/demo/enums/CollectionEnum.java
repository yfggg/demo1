package com.example.demo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum CollectionEnum {

    // 顺序必须从0开始
    PERSONAL_CREDIT_REPORT("个人信用报告", 0),
    PERSONAL_RED_BLACK_LIST("个人红黑名单", 1),
    LEGAL_PERSON_CREDIT_REPORT("法人信用报告", 2),
    LEGAL_PERSON_RED_BLACK_LIST("法人红黑名单", 3);

    @JsonValue
    private String value;
    @EnumValue
    private int code;

    CollectionEnum(String value, int code) {
        this.value = value;
        this.code = code;
    }

    private static Map<Integer, CollectionEnum> valueMap;
    static {
        valueMap = new HashMap<>(CollectionEnum.values().length);
        for (CollectionEnum type : CollectionEnum.values()) {
            valueMap.put(type.getCode(), type);
        }
    }
    public static CollectionEnum fromValue(int code) {
        return valueMap.get(code);
    }

}
