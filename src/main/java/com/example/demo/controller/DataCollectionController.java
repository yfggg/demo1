package com.example.demo.controller;

import cn.hutool.core.util.IdcardUtil;
import cn.hutool.core.util.NumberUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.example.demo.aop.Timer;
import com.example.demo.entity.PersonCreditReport;
import com.example.demo.enums.AreaCodeEnum;
import com.example.demo.exception.IDCardException;
import com.example.demo.utils.EsUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@Api(tags="采集")
@Slf4j
@RestController
@RequestMapping("/collection")
public class DataCollectionController {

    @Autowired
    private EsUtil esUtil;

    private static final String PERSON_CREDIT_REPORT = "person_credit_report-*";
    private static final String PERSON_CREDIT_REPORT_NEW = "person_credit_report_new";

    @Timer
    @ApiOperation(value="核心字段提取")
    @PostMapping(value = "/extraction/coreFields")
    public String extractionOfCoreFields() {
        try {
            if(!esUtil.createIndex(PERSON_CREDIT_REPORT_NEW)) return null;
            esUtil.scrollSearch(PERSON_CREDIT_REPORT, 1000, hit -> handleHit(hit));
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
        String id = null;
        try {
            // 提取 id
            id = searchHit.getId();
            // 提取 _source
            String source = searchHit.getSourceAsString();
            // TODO 对_source(是128位) 进行md5加密
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
                throw new IDCardException("证件格式错误！");
            }
            personCreditReport.setIdentificationNumber(identificationNumber);
            personCreditReport.setName(personalInfo.get("xm").toString());
            personCreditReport.setAccountLocation(personalInfo.get("hkszd").toString());
            personCreditReport.setMollyScore(data.get("xymlf").toString());
            personCreditReport.setCreditLevel(data.get("xydj").toString());
            // 身份证提取地址编码(市 区 县)
            String area = identificationNumber.substring(0, 6);
            personCreditReport.setArea(AreaCodeEnum.fromValue(area).getArea());
            // 身份证提取年龄
            int age = IdcardUtil.getAgeByIdCard(identificationNumber);
            personCreditReport.setAge(String.valueOf(age));

            // 覆盖原始 _source
            Map<String, Object> fields = new HashMap<>();
            fields.put("@timestamp",timestamp);
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
            fields.put("md5", "");
            fields.put("filter", "0");
            fields.put("url", "");
            // 上链必须字段
            fields.put("slsj", "");
            fields.put("txid", "");
            fields.put("info", "");
            esUtil.addData(PERSON_CREDIT_REPORT_NEW, id, fields);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            log.error("_id 为 {} 的用户空指针异常: {}", id, e.getMessage());
        } catch (JSONException e) {
            log.error("_id 为 {} 的用户JSON异常: {}", id, e.getMessage());
        } catch (IDCardException e) {
            log.error("_id 为 {} 的用户身份证异常: {}", id, e.getMessage());
        }
    }


}
