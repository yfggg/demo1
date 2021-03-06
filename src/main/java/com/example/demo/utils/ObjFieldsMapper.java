package com.example.demo.utils;

import com.example.demo.entity.Account;
import com.example.demo.entity.English;
import com.example.demo.entity.Pinyin;
import com.example.demo.vo.AccountVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ObjFieldsMapper {

    ObjFieldsMapper INSTANCE = Mappers.getMapper(ObjFieldsMapper.class);

    @Mapping(source = "pinyin", target = "english")
    English toEnglish(Pinyin pinyin);

    AccountVO toVO(Account account);
}
