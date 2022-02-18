package com.example.demo.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * redis操作工具类.</br>
 * (基于RedisTemplate)
 * @author xcbeyond
 * 2018年7月19日下午2:56:24
 */
@Slf4j
@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 写入缓存
     * @param key
     * @param value
     * @param time
     * @param unit
     * @return
     */
    public boolean set(final String key, String value, long time, TimeUnit unit) {
        boolean result = false;
        try {
            redisTemplate.opsForValue().set(key, value);
            if (time > 0) {
                redisTemplate.expire(key, time, unit);
            }
            result = true;
        } catch (Exception e) {
            log.error("cache save failed! key:[{}] value:[{}]", key, value);
        }
        return result;
    }

    /**
     * 写入缓存
     * @param key
     * @param value
     * @return
     */
    public boolean set(String key, String value) {
        return set(key, value, -1, null);
    }

    /**
     * 根据 key 判断缓存是否存在
     * @param key
     * @return
     */
    public boolean containsKey(String key) {
        boolean result = false;
        try {
            redisTemplate.hasKey(key);
            result = true;
        } catch (Exception e) {
            log.error("contains key failed! key:[{}]", key);
        }
        return result;
    }

    /**
     * 读取缓存
     * @param key
     * @return
     */
    public String get(final String key) {
        if(containsKey(key)) {
            return redisTemplate.opsForValue().get(key);
        }
        return null;
    }

    /**
     * 删除缓存
     * @param key
     * @return
     */
    public boolean delete(final String key) {
        boolean result = false;
        try {
            redisTemplate.delete(key);
            result = true;
        } catch (Exception e) {
            log.error("cache delete failed! key:[{}]", key);
        }
        return result;
    }

}
