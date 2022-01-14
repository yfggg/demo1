package com.example.demo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum ColorEnum {

    // 顺序必须从0开始
    RED("红色", 0),
    GREEN("绿色", 1),
    BLANK("白色", 2),
    YELLO("黄色", 3);

    @JsonValue
    private String value;
    @EnumValue
    private int code;

    ColorEnum(String value, int code) {
        this.value = value;
        this.code = code;
    }

    private static Map<Integer, ColorEnum> valueMap;
    static {
        valueMap = new HashMap<>(ColorEnum.values().length);
        for (ColorEnum type : ColorEnum.values()) {
            valueMap.put(type.getCode(), type);
        }
    }
    public static ColorEnum fromValue(int code) {
        return valueMap.get(code);
    }

}
