package com.example.demo.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IsRepeatSubmit {
    /** 请求间隔时间，单位秒，该时间范围内的请求为重复请求 */
    int intervalTime() default 5;
    /** 返回的提示信息 */
    String msg() default "请不要频繁重复请求！";
}
