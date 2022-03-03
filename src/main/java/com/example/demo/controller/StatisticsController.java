package com.example.demo.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.demo.aop.Timer;
import com.example.demo.entity.Bucket;
import com.example.demo.entity.PersonCreditReport;
import com.example.demo.utils.EsUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.range.DateRangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.SumAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.ValueCount;
import org.elasticsearch.search.aggregations.metrics.ValueCountAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.schema.Entry;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Api(tags="统计")
@Slf4j
@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    @Autowired
    private EsUtil esUtil;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private static final String PERSON_CREDIT_REPORT_NEW = "person_credit_report_new";

    @Timer
    @ApiOperation(value="自然人信用报告上链总数 (按年龄层统计)")
    @PostMapping(value = "/byAgeInterval")
    public Bucket byAgeInterval() {
        try {
            // 上链了才会有 slsj 这个字段
            return esUtil.dateRangeAggregationSubCount(PERSON_CREDIT_REPORT_NEW,
                    "age", "slsj.keyword");
        } catch (NullPointerException e) {
            log.error("field不存在！");
        }
        return null;
    }

    @Timer
    @ApiOperation(value="自然人信用报告上链总数 (按户口所在地统计)")
    @PostMapping(value = "/byArea")
    public Bucket byArea() {
        try {
            // 上链了才会有 slsj 这个字段
            return esUtil.termsAggregationSubCount(PERSON_CREDIT_REPORT_NEW,
                    "area.keyword", "slsj.keyword");
        } catch (NullPointerException e) {
            log.error("field不存在！");
        }
        return null;
    }

    @Timer
    @ApiOperation(value="自然人信用报告上链总数 (按时间统计)")
    @PostMapping(value = "/byTimeRange")
    public Bucket byTimeRange() {
        try {
            return esUtil.dateHistogramAggregationSubCount(PERSON_CREDIT_REPORT_NEW,
                    "2022-01-25", "2022-02-25", "year", "slsj.keyword");
        } catch (NullPointerException e) {
            log.error("field不存在！");
        }
        return null;
    }

    @Timer
    @ApiOperation(value="map2Bean")
    @PostMapping(value = "/map2Bean")
    public String test() {
        List<Map<String, Object>> maps =
                esUtil.search(PERSON_CREDIT_REPORT_NEW, new SearchSourceBuilder(),0,10);

        // map to bean
        List<PersonCreditReport> personCreditReports = maps.stream().map(map -> {
            PersonCreditReport personCreditReport =
                    BeanUtil.mapToBean(map, PersonCreditReport.class, true, new CopyOptions());
            return personCreditReport;
        }).collect(Collectors.toList());

        return null;
    }


}
