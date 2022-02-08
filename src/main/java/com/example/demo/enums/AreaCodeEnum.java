package com.example.demo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum AreaCodeEnum {

    GU_LOU("鼓楼区", "350102"),
    TAI_JIANG("台江区", "350103"),
    CANG_SHAN("仓山区", "350104"),
    MA_WEI("马尾区", "350105"),
    JIN_AN("晋安区", "350111"),
    MIN_HOU("闽侯县", "350121"),
    LIAN_JIANG("连江县", "350122"),
    LUO_YUAN("罗源县", "350123"),
    MIN_QING("闽清县", "350124"),
    YONG_TAI("永泰县", "350125"),
    PING_TAN("平潭县", "350128"),
    FU_QING("福清市", "350181"),
    CHANG_LE("长乐市", "350182"),
    // 撤县设市前的
    FU_QING_OLD("福清市", "350127"),
    CHANG_LE_OLD("长乐市", "350126");

    @JsonValue
    private String code;
    @EnumValue
    private String area;

    AreaCodeEnum(String area, String code) {
        this.code = code;
        this.area = area;
    }

    // 根据code取area
    private static Map<String, AreaCodeEnum> valueMap;
    static {
        valueMap = new HashMap<>(AreaCodeEnum.values().length);
        for (AreaCodeEnum type : AreaCodeEnum.values()) {
            valueMap.put(type.getCode(), type);
        }
    }
    public static AreaCodeEnum fromValue(String code) {
        return valueMap.get(code);
    }

}
