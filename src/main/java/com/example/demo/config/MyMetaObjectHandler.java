package com.example.demo.config;

import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Component
@Slf4j
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("start insert fill ....");
        this.setFieldValByName("isDeleted", "0", metaObject);
        this.setFieldValByName("createTime", new Date(), metaObject);
        this.setFieldValByName("updataTime", new Date(), metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("start insert fill ....");
        this.setFieldValByName("updataTime", new Date(), metaObject);
    }
}

