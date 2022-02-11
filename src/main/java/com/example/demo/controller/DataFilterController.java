//package com.example.demo.controller;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONException;
//import com.alibaba.fastjson.JSONObject;
//import com.example.demo.aop.Timer;
//import com.example.demo.entity.PersonCreditReport;
//import com.example.demo.enums.AreaCodeEnum;
//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiOperation;
//import lombok.extern.slf4j.Slf4j;
//import org.elasticsearch.action.search.*;
//import org.elasticsearch.client.RequestOptions;
//import org.elasticsearch.client.RestHighLevelClient;
//import org.elasticsearch.core.TimeValue;
//import org.elasticsearch.index.query.QueryBuilders;
//import org.elasticsearch.search.Scroll;
//import org.elasticsearch.search.SearchHit;
//import org.elasticsearch.search.SearchHits;
//import org.elasticsearch.search.builder.SearchSourceBuilder;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.util.DigestUtils;
//import org.springframework.util.StringUtils;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.io.IOException;
//import java.io.UnsupportedEncodingException;
//import java.time.ZonedDateTime;
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//
//@Api(tags="过滤")
//@Slf4j
//@RestController
//@RequestMapping("/filter")
//public class DataFilterController {
//
//    @Autowired
//    private RestHighLevelClient restHighLevelClient;
//
//    private static final String PERSON_CREDIT_REPORT = "person_credit_report-*";
//
//    @Timer
//    @ApiOperation(value="合规性处理")
//    @PostMapping(value = "/compliance/process")
//    public String complianceProcess() {
//        List<PersonCreditReport> personCreditReports = new ArrayList<>();
//
//        final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1L));
//        SearchRequest searchRequest = new SearchRequest(PERSON_CREDIT_REPORT);
//        searchRequest.scroll(scroll);
//        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
//        // 控制一次检索多少个结果 避免一次性查询全部影响性能
//        searchSourceBuilder.size(1000);
//        searchRequest.source(searchSourceBuilder);
//
//        try {
//            // 通过发送初始 SearchRequest 来初始化搜索
//            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
//            String scrollId = searchResponse.getScrollId();
//            SearchHit[] searchHits = searchResponse.getHits().getHits();
//            iterateOverHits(personCreditReports, searchHits);
//            // 通过在循环中调用 Search Scroll api 来检索所有搜索命中 直到没有文档返回
//            while (searchHits != null && searchHits.length > 0) {
//                // 创建一个新的 SearchScrollRequest 保存最后返回的滚动标识符
//                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
//                scrollRequest.scroll(scroll);
//                searchResponse = restHighLevelClient.scroll(scrollRequest, RequestOptions.DEFAULT);
//                scrollId = searchResponse.getScrollId();
//                searchHits = searchResponse.getHits().getHits();
//                iterateOverHits(personCreditReports, searchHits);
//            }
//
//            // 完成后清除滚动
//            ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
//            clearScrollRequest.addScrollId(scrollId);
//            ClearScrollResponse clearScrollResponse = restHighLevelClient.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
//            boolean succeeded = clearScrollResponse.isSucceeded();
//            // 打印清除结果
//            log.info(String.valueOf(succeeded));
//
//        } catch (IOException e) {
//            log.error("合规性处理失败！");
//        }
//
//        return null;
//    }
//
//    /**
//     * 遍历 Hits
//     *
//     * @param personCreditReports
//     * @param searchHits
//     */
//    private void iterateOverHits(List<PersonCreditReport> personCreditReports, SearchHit[] searchHits) {
//        for (SearchHit searchHit : searchHits) {
//            // 验证json格式
//            try {
////                String json = JSON.parseObject(searchHit.getSourceAsString()).get("json").toString();
////                String data = JSON.parseObject(json).get("data").toString();
////                // 判断 data 是否为空
////                if(JSON.parseObject(data).get("grbgtgxx") == null) { continue; }
////                String personalInfo = JSON.parseObject(data).get("grbgtgxx").toString();
////                PersonCreditReport personCreditReport = extractionOfCoreFields(JSON.parseObject(personalInfo));
////                personCreditReports.add(personCreditReport);
//            } catch (JSONException e) { continue; }
//        }
//    }
//
//    /**
//     * 核心字段的提取，提取过程中对字段的合规性进行校验
//     *
//     * @param personalInfo
//     * @return
//     */
//    public PersonCreditReport extractionOfCoreFields(JSONObject personalInfo) {
//        String md5;
//        String identificationNumber;
//        String area;
//        String name;
//        Integer age;
//
//        // 对原始json进行md5加密
//        try {
//            // 32位小写加密
//            md5 = DigestUtils.md5DigestAsHex(personalInfo.toString().getBytes("utf-8"));
//        } catch (UnsupportedEncodingException e) {
//            log.error("md5加密失败！");
//            return null;
//        }
//
//        // 对象之间拼音到英文的映射
//        PersonCreditReport personCreditReport = new PersonCreditReport();
//        try{
//            identificationNumber = personalInfo.get("sfzhm").toString();
//            name = personalInfo.get("xm").toString();
//        } catch (NullPointerException e) {
//            log.error("姓名或者身份证号码为空！");
//            return null;
//        }
//        personCreditReport.setIdentificationNumber(identificationNumber);
//        personCreditReport.setName(name);
//        personCreditReport.setMd5(md5);
//
//        // 身份证提取地址编码(市 区 县)
////        try{
////            area = identificationNumber.substring(0, 6);
////        } catch (IndexOutOfBoundsException e) {
////            log.error("身份证号码格式错误！");
////            return null;
////        }
////        personCreditReport.setArea(AreaCodeEnum.fromValue(area).getArea());
//
//        // 身份证提取年龄
////        try{
////            age = ZonedDateTime.now().getYear() - Integer.valueOf(identificationNumber.substring(6, 10)).intValue();
////            personCreditReport.setAge(age);
////        } catch (IndexOutOfBoundsException e) {
////            log.error("身份证号码格式错误！");
////            return null;
////        }
//
//        // TODO 发现不合规的对其进行保存数据库处理
//        // TODO 更新es上对应数据的标记
//        // TODO 聚合运算
//
//        return personCreditReport;
//    }
//
//}
