package com.example.demo.controller;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.IdcardUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.aop.Timer;
import com.example.demo.entity.CollectionException;
import com.example.demo.entity.PersonCreditReport;
import com.example.demo.enums.AreaCodeEnum;
import com.example.demo.enums.CollectionEnum;
import com.example.demo.service.ICollectionExceptionService;
import com.example.demo.utils.EsUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Api(tags="采集")
@Slf4j
@RestController
@RequestMapping("/collection")
public class CollectionController {

    @Autowired
    private EsUtil esUtil;

    @Autowired
    private ICollectionExceptionService collectionExceptionService;

    private static final String PERSON_CREDIT_REPORT = "person_credit_report-*";
    private static final String PERSON_CREDIT_REPORT_NEW = "person_credit_report_new";

    @Timer
    @ApiOperation(value="核心字段提取")
    @PostMapping(value = "/extractionOfCoreFields")
    public String extractionOfCoreFields() {
        try {
            if(!esUtil.createIndex(PERSON_CREDIT_REPORT_NEW)) return null;

            esUtil.scrollSearch(PERSON_CREDIT_REPORT, 1000, hit -> handleHit(hit));

            // 根据mysql的查询记录更新es的 filter标记
            QueryWrapper<CollectionException> queryWrapper = new QueryWrapper<>();
            queryWrapper.and(
                    wrapper -> wrapper.eq("del_flag", "0")
            );
            List<CollectionException> collectionExceptions = collectionExceptionService.list(queryWrapper);
            if(!collectionExceptions.isEmpty()) {
                List<String> docIds = collectionExceptions.stream()
                        .map(collectionException -> collectionException.getDocId())
                        .collect(Collectors.toList());

                Map<String, Object> fields = new HashMap<>();
                fields.put("filter", "1");
                esUtil.bulkUpdateDataById(PERSON_CREDIT_REPORT_NEW, docIds, fields);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 处理 Hit
     * @param searchHit
     */
    private void handleHit(SearchHit searchHit) {
        String docId = null;
        try {
            // 提取 doc_id
            docId = searchHit.getId();
            // 提取 _source
            String source = searchHit.getSourceAsString();
            // TODO 对_source(是128位) 进行md5加密
            String md5 = DigestUtils.md5DigestAsHex(source.getBytes("utf-8"));
            // TODO 对_source(是128位) 进行base64加密
            String base64 = Base64.encode(source);
            // 提取 @timestamp
            String timestamp = JSON.parseObject(searchHit.getSourceAsString()).get("@timestamp").toString();
            // 提取 json
            String json = JSON.parseObject(searchHit.getSourceAsString()).get("json").toString();
            // 提取 data
            JSONObject data = (JSONObject) JSON.parseObject(json).get("data");
            // 提取个人信息
            JSONObject personalInfo = (JSONObject) data.get("grbgtgxx");
            // 封装
            PersonCreditReport personCreditReport = new PersonCreditReport();
            personCreditReport.setId(personalInfo.get("id").toString());
            String identificationNumber = personalInfo.get("sfzhm").toString();
            // 验证身份证是否合法
            if(!IdcardUtil.isValidCard(identificationNumber)) {
                log.error("_id 为 {} 的用户身份证格式异常！", docId);
                // 有问题保存到 mysql
                CollectionException ce = collectionExceptionService.getById(docId);
                // 存在记录就更新描述
                Optional.ofNullable(ce)
                        .map(collectionException -> collectionException.getExceptionDescription())
                        .ifPresent(exceptionDescription -> {
                            StringBuilder sb = new StringBuilder(exceptionDescription);
                            String description = StrUtil.format("身份证格式异常: {}, ", identificationNumber);
                            sb.append(description);
                            ce.setExceptionDescription(sb.toString());
                            collectionExceptionService.saveOrUpdate(ce);
                        });
                // 不存在记录就添加记录
                String finalDocId = docId;
                Optional.ofNullable(ce)
                        .orElseGet(() -> {
                            CollectionException collectionException = new CollectionException();
                            collectionException.setDocId(finalDocId);
                            collectionException.setDocIndex(PERSON_CREDIT_REPORT);
                            collectionException.setDataType(CollectionEnum.PERSONAL_CREDIT_REPORT);
                            String description = StrUtil.format("身份证格式异常: {}, ", identificationNumber);
                            collectionException.setExceptionDescription(description);
                            collectionExceptionService.saveOrUpdate(collectionException);
                            return null;
                        });
            }
            personCreditReport.setMd5(md5);
            personCreditReport.setIdentificationNumber(identificationNumber);
            personCreditReport.setName(personalInfo.get("xm").toString());
            personCreditReport.setAccountLocation(personalInfo.get("hkszd").toString());
            personCreditReport.setMollyScore(new Double(data.get("xymlf").toString()));
            personCreditReport.setCreditLevel(data.get("xydj").toString());
            // 身份证提取地址编码(市 区 县)
            String area = identificationNumber.substring(0, 6);
            personCreditReport.setArea(AreaCodeEnum.fromValue(area).getArea());
            // 身份证提取年龄
            int age = IdcardUtil.getAgeByIdCard(identificationNumber);
            personCreditReport.setAge(age);

            // 覆盖原始 _source
            Map<String, Object> fields = new HashMap<>();
            fields.put("@timestamp", timestamp);
            // 源字段
            fields.put("id", personCreditReport.getId());
            fields.put("name", personCreditReport.getName());
            fields.put("identification_number", personCreditReport.getIdentificationNumber());
            fields.put("molly_score", personCreditReport.getMollyScore());
            fields.put("credit_level", personCreditReport.getCreditLevel());
            fields.put("account_location", personCreditReport.getAccountLocation());
            fields.put("area", personCreditReport.getArea());
            fields.put("age", personCreditReport.getAge());
            // 后期增加字段
            fields.put("batch_id", "111");
            fields.put("filter", "0");
            fields.put("url", "");
            // 上链必须字段
            fields.put("slsj", "");
            fields.put("txid", "");
            fields.put("info", "");
            // 源数据备份
            fields.put("base64", base64);
            esUtil.addData(PERSON_CREDIT_REPORT_NEW, docId, fields);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            log.error("_id 为 {} 的用户空指针异常: {}", docId, e.getMessage());
        } catch (JSONException e) {
            log.error("_id 为 {} 的用户JSON异常: {}", docId, e.getMessage());
        }
    }

}
