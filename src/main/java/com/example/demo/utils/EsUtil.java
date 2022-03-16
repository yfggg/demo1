package com.example.demo.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.example.demo.entity.Bucket;
import com.example.demo.entity.TestData;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.*;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.ExtendedBounds;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.range.DateRangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Consumer;

/**
 * es 的工具类
 * @author yangfan
 */
@Slf4j
@Component
public class EsUtil {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private BulkProcessor bulkProcessor;

    /**
     * 创建索引
     *
     * @param index 索引
     * @retur
     */
    public boolean createIndex(String index) {
        if(isIndexExist(index)){
            log.error(StrUtil.format("索引 {} 已经存在！", index));
            return false;
        }
        try {
            CreateIndexRequest request = new CreateIndexRequest(index);
            CreateIndexResponse response = restHighLevelClient.indices().create(request, RequestOptions.DEFAULT);
            return response.isAcknowledged();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 判断索引是否存在
     *
     * @param index
     * @return
     */
    public boolean isIndexExist(final String index) {
        try {
            GetIndexRequest request = new GetIndexRequest(index);
            return restHighLevelClient.indices().exists(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 删除索引
     *
     * @param index
     * @return
     */
    public boolean deleteIndex(String index)  {
        if(!isIndexExist(index)){
            log.error(StrUtil.format("索引 {} 不存在！", index));
            return false;
        }
        try {
            DeleteIndexRequest request = new DeleteIndexRequest(index);
            AcknowledgedResponse delete = restHighLevelClient.indices().delete(request, RequestOptions.DEFAULT);
            return delete.isAcknowledged();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 批量插入 随机ID
     *
     * @param index
     * @param
     */
    public<T> void bulkAddData(String index, List<T> list) {
        List<IndexRequest> indexRequests = new ArrayList<>();
        list.forEach(e -> {
            IndexRequest request = new IndexRequest(index);
            request.id(IdUtil.simpleUUID());
            request.source(JSON.toJSONString(e), XContentType.JSON);
            request.opType(DocWriteRequest.OpType.CREATE);
            indexRequests.add(request);
        });
        indexRequests.forEach(bulkProcessor::add);
    }

    /**
     * 通过ID删除数据
     *
     * @param index
     * @param id
     */
    public void deleteDataById(String index, String id) {
        try {
            DeleteRequest request = new DeleteRequest(index, id);
            DeleteResponse response = restHighLevelClient.delete(request, RequestOptions.DEFAULT);
            if(!response.status().equals(RestStatus.OK)) {
                log.warn("删除 _id:{} 的数据失败", id);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 通过ID获取数据
     *
     * @param index  索引，类似数据库
     * @param id     数据ID
     * @return
     */
    public Map<String,Object> searchById(String index, String id) {
        GetRequest request = new GetRequest(index, id);
        request.fetchSourceContext(new FetchSourceContext(true, null, Strings.EMPTY_ARRAY));
        GetResponse response;
        try {
            response = restHighLevelClient.get(request, RequestOptions.DEFAULT);
            return response.getSource();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 滚动查询 一般用于数据迁移or索引变更
     *
     * @param index
     * @param size
     * @param consumer
     * @throws IOException
     */
    public void scrollSearch(String index, Integer size, Consumer<SearchHit> consumer) {
        try {
            // 存活一分钟
            final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1L));
            SearchRequest searchRequest = new SearchRequest(index);
            searchRequest.scroll(scroll);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            // 控制一页检索多少个结果 避免一次性查询全部影响性能
            searchSourceBuilder.size(size);
            searchRequest.source(searchSourceBuilder);

            // 通过发送初始 SearchRequest 来初始化搜索
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            String scrollId = searchResponse.getScrollId();
            SearchHit[] searchHits = searchResponse.getHits().getHits();
            for (SearchHit searchHit : searchHits) {
                consumer.accept(searchHit);
            }

            // 通过在循环中调用 Search Scroll api 来检索所有搜索命中 直到没有文档返回
            while (searchHits != null && searchHits.length > 0) {
                // 创建一个新的 SearchScrollRequest 保存最后返回的滚动标识符
                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(scroll);
                searchResponse = restHighLevelClient.scroll(scrollRequest, RequestOptions.DEFAULT);
                scrollId = searchResponse.getScrollId();
                searchHits = searchResponse.getHits().getHits();
                for (SearchHit searchHit : searchHits) {
                    consumer.accept(searchHit);
                }
            }

            // 完成后清除滚动
            ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
            clearScrollRequest.addScrollId(scrollId);
            ClearScrollResponse clearScrollResponse = restHighLevelClient.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
            boolean succeeded = clearScrollResponse.isSucceeded();
            // 打印清除结果
            log.info(String.valueOf(succeeded));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 分页查询
     *
     * @param index
     * @param query
     * @param size
     * @param from
     * @return
     * @throws IOException
     */
    public List<Map<String, Object>> search(String index,
                                            SearchSourceBuilder query,
                                            Integer from, Integer size) {
        try {
            SearchRequest searchRequest = new SearchRequest(index);
            SearchSourceBuilder searchSourceBuilder = query;
            searchSourceBuilder.from(from);
            searchSourceBuilder.size(size);
            searchRequest.source(searchSourceBuilder);
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            // 查询成功
            if (200 == searchResponse.status().getStatus()) {
                // 解析对象
                List<Map<String,Object>> list = new ArrayList<>();
                for (SearchHit hit : searchResponse.getHits().getHits()) {
                    hit.getSourceAsMap().put("id", hit.getId());
                    list.add(hit.getSourceAsMap());
                }
                return list;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * range范围桶聚合
     *
     * @param index
     * @param dateRangeField
     * @return
     * @throws IOException
     */
    public Bucket dateRangeAggregationSubCount(String index, String dateRangeField) {
        try {
            // 创建一个查询请求，并指定索引名称
            SearchRequest searchRequest = new SearchRequest(index);

            // 按 dateRangeField 范围统计
            DateRangeAggregationBuilder dateRangeAggregationBuilder =
                    AggregationBuilders.dateRange("date_range").field(dateRangeField)
                            .addRange(0, 20).addRange(20, 40).addRange(40, 60)
                            .addRange(60, 80).addRange(80, 100).addRange(100, 120);

            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.aggregation(dateRangeAggregationBuilder);
            searchRequest.source(searchSourceBuilder);

            // 发起请求，获取响应结果
            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            // 获取聚合的结果
            Range buckets = response.getAggregations().get("date_range");

            Bucket result = new Bucket();
            List<String> datas = new ArrayList<>();
            List<String> values = new ArrayList<>();

            // 循环遍历各个桶结果
            for (Range.Bucket bucket : buckets.getBuckets()) {
                datas.add(bucket.getKeyAsString());
                values.add(String.valueOf(bucket.getDocCount()));
            }

            result.setDates(datas);
            result.setValues(values);

            return result;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            log.error("传参错误！");
        }
        return null;
    }

    /**
     * term自定义分组桶聚合
     *
     * @param index
     * @param termsField
     * @return
     * @throws IOException
     */
    public Bucket termsAggregationSubCount(String index, String termsField) {
        try {
            // 创建一个查询请求，并指定索引名称
            SearchRequest searchRequest = new SearchRequest(index);

            // 按 termsField 范围统计
            TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms("terms").field(termsField);

            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.aggregation(termsAggregationBuilder);
            searchRequest.source(searchSourceBuilder);

            // 发起请求，获取响应结果
            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            // 获取聚合的结果
            Terms buckets = response.getAggregations().get("terms");

            Bucket result = new Bucket();
            List<String> datas = new ArrayList<>();
            List<String> values = new ArrayList<>();

            // 循环遍历各个桶结果
            for (Terms.Bucket bucket : buckets.getBuckets()) {
                datas.add(bucket.getKeyAsString());
                values.add(String.valueOf(bucket.getDocCount()));
            }

            result.setDates(datas);
            result.setValues(values);

            return result;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            log.error("传参错误！");
        }
        return null;
    }

    /**
     * 日期直方图桶聚合
     *
     * @param index
     * @param startTime
     * @param endTime
     * @param timeDimension
     * @param dateHistogramField
     * @param boolQueryBuilder
     * @return
     */
    public Bucket dateHistogramAggregationSubCount(String index,
                                                   String startTime, String endTime,
                                                   Object timeDimension, String dateHistogramField,
                                                   BoolQueryBuilder boolQueryBuilder) {
        try {
            // 创建一个查询请求，并指定索引名称
            SearchRequest searchRequest = new SearchRequest(index);

            // 按时间范围统计
            DateHistogramAggregationBuilder dateHistogramAggregationBuilder = AggregationBuilders.dateHistogram("dh");
            dateHistogramAggregationBuilder.field(dateHistogramField);
            switch(Integer.parseInt(timeDimension.toString())){
                case 1 :
                    dateHistogramAggregationBuilder.calendarInterval(DateHistogramInterval.DAY);
                    dateHistogramAggregationBuilder.format("yyyy-MM-dd");
                    break;
                case 7 :
                    dateHistogramAggregationBuilder.calendarInterval(DateHistogramInterval.WEEK);
                    dateHistogramAggregationBuilder.format("yyyy-MM-dd");
                    break;
                case 30 :
                    dateHistogramAggregationBuilder.calendarInterval(DateHistogramInterval.MONTH);
                    dateHistogramAggregationBuilder.format("yyyy-MM");
                    break;
                case 365 :
                    dateHistogramAggregationBuilder.calendarInterval(DateHistogramInterval.YEAR);
                    dateHistogramAggregationBuilder.format("yyyy");
                    break;
                default :
                    throw new NullPointerException();
            }
            dateHistogramAggregationBuilder.timeZone(ZoneId.of("Asia/Shanghai"));

            // 柱状图显示范围
            dateHistogramAggregationBuilder.extendedBounds(
                    new ExtendedBounds(
                            DateUtil.parse(startTime).getTime(), DateUtil.parse(endTime).getTime()
                    ));

            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);
            searchSourceBuilder.aggregation(dateHistogramAggregationBuilder);
            searchRequest.source(searchSourceBuilder);

            // 发起请求，获取响应结果
            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            // 获取聚合的结果
            Histogram buckets = response.getAggregations().get("dh");

            Bucket result = new Bucket();
            List<String> datas = new ArrayList<>();
            List<String> values = new ArrayList<>();
            List<String> chainGrowthRate = new ArrayList<>();

            // 循环遍历各个桶结果
            int thisIssue = 1;
            int lastIssue;
            for (Histogram.Bucket bucket : buckets.getBuckets()) {
                datas.add(bucket.getKeyAsString());
                values.add(String.valueOf(bucket.getDocCount()));
                lastIssue = thisIssue;
                thisIssue += new Long(bucket.getDocCount()).intValue();
                // 环比增长率 = (本期数 - 上期数) / 上期数 × 100%
                BigDecimal diff = new BigDecimal(thisIssue - lastIssue);
                BigDecimal rate = BigDecimal.ZERO;
                if(0 != diff.compareTo(BigDecimal.ZERO)) {
                    rate = diff.divide(new BigDecimal(lastIssue)).multiply(new BigDecimal(100))
                            .setScale(1, RoundingMode.HALF_UP);
                }
                chainGrowthRate.add(new StringBuilder().append(rate).append("%").toString());
            }

            result.setDates(datas);
            result.setValues(values);
            result.setChainGrowthRate(chainGrowthRate);

            return result;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            log.error("传参错误！");
        } catch (ElasticsearchStatusException e) {
            log.error("检查开始和结束时间！");
        }
        return null;
    }

}


