package com.example.demo.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.example.demo.aop.Timer;
import com.example.demo.entity.English;
import com.example.demo.entity.PersonCreditReport;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.*;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.parameters.P;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;

@Api(tags="采集")
@Slf4j
@RestController
@RequestMapping("/collection")
public class DataCollectionController {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    private static final String PERSON_CREDIT_REPORT = "person_credit_report-*";
    private static final String PERSON_CREDIT_REPORT_NEW = "person_credit_report_new";

    @Timer
    @ApiOperation(value="核心字段提取")
    @PostMapping(value = "/extraction/coreFields")
    public String extractionOfCoreFields() {

        // 如果索引不存在
        if(!existIndex(PERSON_CREDIT_REPORT_NEW)) {
            // 创建新索引
            createIndex(PERSON_CREDIT_REPORT_NEW);
        }

        final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1L));
        SearchRequest searchRequest = new SearchRequest(PERSON_CREDIT_REPORT);
        searchRequest.scroll(scroll);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 控制一次检索多少个结果 避免一次性查询全部影响性能
        searchSourceBuilder.size(1000);
        searchRequest.source(searchSourceBuilder);

        try {
            // 通过发送初始 SearchRequest 来初始化搜索
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            String scrollId = searchResponse.getScrollId();
            SearchHit[] searchHits = searchResponse.getHits().getHits();
            iterateOverHits(searchHits);

            // 通过在循环中调用 Search Scroll api 来检索所有搜索命中 直到没有文档返回
            while (searchHits != null && searchHits.length > 0) {
                // 创建一个新的 SearchScrollRequest 保存最后返回的滚动标识符
                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(scroll);
                searchResponse = restHighLevelClient.scroll(scrollRequest, RequestOptions.DEFAULT);
                scrollId = searchResponse.getScrollId();
                searchHits = searchResponse.getHits().getHits();
                iterateOverHits(searchHits);
            }

            // 完成后清除滚动
            ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
            clearScrollRequest.addScrollId(scrollId);
            ClearScrollResponse clearScrollResponse = restHighLevelClient.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
            boolean succeeded = clearScrollResponse.isSucceeded();
            // 打印清除结果
            log.info(String.valueOf(succeeded));

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
     * 判断索引是否存在
     * @param index
     * @throws IOException
     */
    private boolean existIndex(final String index) {
        try {
            GetIndexRequest request = new GetIndexRequest(index);
            return restHighLevelClient.indices().exists(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("判断索引 {} 是否存在失败！", index);
        }
        return false;
    }

    /**
     * 创建新索引
     */
    private void createIndex(final String index) {
        try {
            // 1、创建索引请求
            CreateIndexRequest createIndexRequest = new CreateIndexRequest(index);
            // 2、客户端执行请求 IndicesClient,请求后获得响应
            CreateIndexResponse createIndexResponse
                    = restHighLevelClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
            log.info(createIndexResponse.toString());
        } catch (IOException e) {
            log.error("创建索引 {} 失败！", index);
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
            log.error("覆盖原始 _source 失败！");
        }
    }

}
