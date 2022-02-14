package com.example.demo.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.demo.aop.Timer;
import com.example.demo.entity.PersonCreditReport;
import com.example.demo.utils.EsUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.function.Consumer;

@Api(tags="采集")
@Slf4j
@RestController
@RequestMapping("/collection")
public class DataCollectionController {

    @Autowired
    private EsUtil esUtil;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    private static final String PERSON_CREDIT_REPORT = "person_credit_report-*";
    private static final String PERSON_CREDIT_REPORT_NEW = "person_credit_report_new";



    @Timer
    @ApiOperation(value="核心字段提取")
    @PostMapping(value = "/extraction/coreFields")
    public String extractionOfCoreFields() {
        try {
            if(!esUtil.createIndex(PERSON_CREDIT_REPORT_NEW)) return null;
            esUtil.scrollSearch(PERSON_CREDIT_REPORT, 1000, e -> iterateOverHits(e));
        } catch (IOException e) {
            log.error("核心字段提取失败！");
        }
        return null;
    }



    /**
     * 遍历 Hits
     * @param searchHits
     */
    private void iterateOverHits(SearchHit[] searchHits) {
        for (SearchHit searchHit : searchHits) {
            // 验证json格式
            try {
                // 提取 id
                String id = searchHit.getId();
                // 提取 _source
                String source = searchHit.getSourceAsString();
                // TODO 对_source(是128位) 进行md5加密
                // 提取 @timestamp
                String timestamp = JSON.parseObject(searchHit.getSourceAsString()).get("@timestamp").toString();
                // 提取 json
                String json = JSON.parseObject(searchHit.getSourceAsString()).get("json").toString();
                // 提取 data
                JSONObject data = (JSONObject) JSON.parseObject(json).get("data");
                // 判断 data 是否为空
                if (data.get("grbgtgxx") == null) { continue; }
                // 提取个人信息
                JSONObject personalInfo = (JSONObject) data.get("grbgtgxx");
                // 封装
                PersonCreditReport personCreditReport = new PersonCreditReport();
                personCreditReport.setId(personalInfo.get("id").toString());
                personCreditReport.setIdentificationNumber(personalInfo.get("sfzhm").toString());
                personCreditReport.setName(personalInfo.get("xm").toString());
                personCreditReport.setAccountLocation(personalInfo.get("hkszd").toString());
                personCreditReport.setMollyScore(data.get("xymlf").toString());
                personCreditReport.setCreditLevel(data.get("xydj").toString());

                // 覆盖原始 _source
                createDocumentSource(id, timestamp, personCreditReport);

            } catch (Exception e) {
                continue;
            }
        }
    }



    /**
     * 覆盖原始 _source
     * @param id
     * @param timestamp
     * @param personCreditReport
     */
    private void createDocumentSource(String id, String timestamp, PersonCreditReport personCreditReport) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                builder.field("@timestamp", timestamp);

                // 源字段
                builder.field("id", personCreditReport.getId());
                builder.field("name", personCreditReport.getName());
                builder.field("identification_number", personCreditReport.getIdentificationNumber());
                builder.field("molly_score", personCreditReport.getMollyScore());
                builder.field("credit_level", personCreditReport.getCreditLevel());
                builder.field("account_location", personCreditReport.getAccountLocation());

                // 后期增加字段
                builder.field("batch_id", "111");
                builder.field("md5", "");
                builder.field("filter", "0");
                builder.field("url", "");

                // 上链必须字段
                builder.field("slsj", "");
                builder.field("txid", "");
                builder.field("info", "");
                builder.field("ledger", "");
            }
            builder.endObject();
            // 文档 source 提供一个 XContent 生成器对象,Elasticsearch内置的帮手来生成 JSON 内容
            IndexRequest indexRequest = new IndexRequest(PERSON_CREDIT_REPORT_NEW).id(id).source(builder);
            IndexResponse indexResponse = restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
            log.info(indexResponse.toString());
        } catch(IOException e) {
            log.error("id {} 覆盖原始 _source 失败！", id);
        }
    }


}
