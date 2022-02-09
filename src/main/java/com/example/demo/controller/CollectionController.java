package com.example.demo.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.example.demo.aop.Timer;
import com.example.demo.entity.PersonCreditReport;
import com.example.demo.enums.AreaCodeEnum;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Api(tags="采集")
@Slf4j
@RestController
@RequestMapping("/collection")
public class CollectionController {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    private static final String PERSON_CREDIT_REPORT = "person_credit_report-*";

    /**
     * 最多一年的量
     */
    @Timer
    @ApiOperation(value="过滤字段")
    @PostMapping(value = "/filterField")
    public String esQuery() throws IOException {

        List<PersonCreditReport> personCreditReports = new ArrayList<>();

        // es查询
        SearchRequest searchRequest = new SearchRequest(PERSON_CREDIT_REPORT);
        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        SearchHit[] searchHits = response.getHits().getHits();

        // 遍历查询结果
        for (SearchHit searchHit : searchHits) {
            String json = JSON.parseObject(searchHit.getSourceAsString()).get("json").toString();
            String data = JSON.parseObject(json).get("data").toString();
            String personalInfo = JSON.parseObject(data).get("grbgtgxx").toString();
            PersonCreditReport personCreditReport = filterField(personalInfo);
            personCreditReports.add(personCreditReport);
        }

        return personCreditReports.toString();
    }

    /**
     * 身份证号码的格式：610821-20061222-612-X 由18位数字组成：前6位为地址码，第7至14位为出生日期码，第15至17位为顺序码，
     * 第18位为校验码。检验码分别是0-10共11个数字，当检验码为“10”时，为了保证公民身份证号码18位，所以用“X”表示。虽然校验码为“X”不能更换，但若需全用数字表示，只需将18位公民身份号码转换成15位居民身份证号码，去掉第7至8位和最后1位3个数码。
     * 当今的身份证号码有15位和18位之分。1985年我国实行居民身份证制度，当时签发的身份证号码是15位的，1999年签发的身份证由于年份的扩展（由两位变为四位）和末尾加了效验码，就成了18位。
     * （1）前1、2位数字表示：所在省份的代码；
     * （2）第3、4位数字表示：所在城市的代码；
     * （3）第5、6位数字表示：所在区县的代码；
     * （4）第7~14位数字表示：出生年、月、日；
     * （5）第15、16位数字表示：所在地的派出所的代码；
     * （6）第17位数字表示性别：奇数表示男性，偶数表示女性
     * （7）第18位数字是校检码：根据一定算法生成
     */
    public PersonCreditReport filterField(String json) {

        // 公用字段
        String md5;
        String identificationNumber;
        String area;
        String name;
        Integer age;
        JSONObject jsonObject;

        // 验证json是否为空
        if(!StringUtils.hasLength(json)) {
            log.error("json is null !");
            return null;
        }

        // 对原始json进行md5加密
        try {
            // 32位小写加密
            md5 = DigestUtils.md5DigestAsHex(json.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            log.error("md5 conver failed !");
            return null;
        }

        // json转对象
        try {
            jsonObject = JSON.parseObject(json);
        } catch (JSONException e) {
            // 验证json格式
            log.error("json format exception !");
            return null;
        }

        // 对象之间拼音到英文的映射
        PersonCreditReport personCreditReport = new PersonCreditReport();
        try{
            identificationNumber = jsonObject.get("sfzhm").toString();
            name = jsonObject.get("xm").toString();
        } catch (NullPointerException e) {
            log.error("identification number or xm is null !");
            return null;
        }
        personCreditReport.setIdentificationNumber(identificationNumber);
        personCreditReport.setName(name);
        personCreditReport.setMd5(md5);

        // 身份证提取地址编码(市 区 县)
        try{
            area = identificationNumber.substring(0, 6);
        } catch (IndexOutOfBoundsException e) {
            log.error("identification number index out !");
            return null;
        }
        personCreditReport.setArea(AreaCodeEnum.fromValue(area).getArea());

        // 身份证提取年龄
        age = ZonedDateTime.now().getYear() - Integer.valueOf(identificationNumber.substring(6, 10)).intValue();
        personCreditReport.setAge(age);

        return personCreditReport;
    }

}
