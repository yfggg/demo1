package com.example.demo.service.impl;

import com.example.demo.entity.English;
import com.example.demo.entity.Pinyin;
import com.example.demo.utils.ObjFieldsMapper;
import org.springframework.stereotype.Service;

@Service
public class FuZhouServiceImpl {

    /**
     * 拼音转英文
     * @return
     */
    public String pinyinToEnglish() {
        Pinyin pinyin = new Pinyin();
        pinyin.setId("1");
        pinyin.setPinyin("abc");
        // 属性复制
        English english = ObjFieldsMapper.INSTANCE.to(pinyin);
        return english.toString();
    }

}
