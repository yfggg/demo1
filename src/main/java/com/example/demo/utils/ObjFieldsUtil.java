package com.example.demo.utils;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;

public class ObjFieldsUtil {

    /**
     * 判断对象所有属性是否全部为空
     *
     * @param object
     * @return
     */
    public static boolean checkObjFieldsIsNull(Object object) {
        if (null == object) {
            return true;
        }
        try {
            for (Field f : object.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                if (f.get(object) != null && !StringUtils.isEmpty(f.get(object).toString())) {
                    return false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}
