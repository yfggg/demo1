package com.example.demo.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.example.demo.entity.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.*;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.histogram.LongBounds;
import org.elasticsearch.search.aggregations.bucket.range.DateRangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.ValueCount;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
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
     * 数据添加 指定ID
     *
     * @param index
     * @param id
     * @param fields
     * @return
     * @throws IOException
     */
    public void addData(String index, String id, Map<String, Object> fields) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            for(String key : fields.keySet()){
                builder.field(key, fields.get(key));
            }
            builder.endObject();
            IndexRequest request = new IndexRequest(index).id(id).source(builder);
            restHighLevelClient.index(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 通过ID 批量更新数据
     *
     * @param fields     要增加的数据
     * @param index      索引，类似数据库
     * @param ids        数据ID列表
     * @return
     */
    public void bulkUpdateDataById(String index, List<String> ids, Map<String, Object> fields) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            for(String key : fields.keySet()){
                String value = fields.get(key).toString();
                builder.field(key, value);
            }
            builder.endObject();

            BulkRequest request = new BulkRequest();
            for(String id : ids) {
                request.add(new UpdateRequest(index, id).doc(builder));
                request.timeout("2m");
            }
            BulkResponse bulkResponse = restHighLevelClient.bulk(request, RequestOptions.DEFAULT);

            for (BulkItemResponse bulkItemResponse : bulkResponse) {
                if (bulkItemResponse.isFailed()) {
                    BulkItemResponse.Failure failure = bulkItemResponse.getFailure();
                    log.error("_id {} 更新失败: {}", bulkItemResponse.getId(), failure.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            if (searchResponse.status().getStatus() == 200) {
                // 解析对象
                List<Map<String,Object>> list = new ArrayList<>();
                for (SearchHit hit : searchResponse.getHits().getHits()) {
                    hit.getSourceAsMap().put("id", hit.getId());
                    // 下划线转驼峰
                    Map<String, Object> hump = new HashMap<>();
                    hit.getSourceAsMap().forEach((k,v) -> hump.put(StrUtil.toCamelCase(k), v));
                    list.add(hump);
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
     * @param countField
     * @return
     * @throws IOException
     */
    public Bucket dateRangeAggregationSubCount(String index, String dateRangeField, String countField) {
        try {
            // 创建一个查询请求，并指定索引名称
            SearchRequest searchRequest = new SearchRequest(index);

            // 按 dateRangeField 范围统计
            DateRangeAggregationBuilder dateRangeAggregationBuilder =
                    AggregationBuilders.dateRange("date_range").field(dateRangeField)
                            .addRange(0, 20).addRange(20, 40).addRange(40, 60)
                            .addRange(60, 80).addRange(80, 100).addRange(100, 120);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            dateRangeAggregationBuilder.subAggregation(AggregationBuilders.count("count").field(countField));
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
                ValueCount valueCount = bucket.getAggregations().get("count");
                datas.add(bucket.getKeyAsString());
                values.add(String.valueOf(valueCount.getValue()));
            }
            result.setDates(datas);
            result.setValues(values);

            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * term自定义分组桶聚合
     *
     * @param index
     * @param termsField
     * @param countField
     * @return
     * @throws IOException
     */
    public Bucket termsAggregationSubCount(String index, String termsField, String countField) {
        try {
            // 创建一个查询请求，并指定索引名称
            SearchRequest searchRequest = new SearchRequest(index);

            // 按 termsField 范围统计
            TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms("terms").field(termsField);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            termsAggregationBuilder.subAggregation(AggregationBuilders.count("count").field(countField));
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
                ValueCount valueCount = bucket.getAggregations().get("count");
                datas.add(bucket.getKeyAsString());
                values.add(String.valueOf(valueCount.getValue()));
            }
            result.setDates(datas);
            result.setValues(values);

            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 日期直方图桶聚合
     *
     * @param startTime
     * @param endTime
     * @param index
     * @param countField
     * @return
     * @throws IOException
     */
    public Bucket dateHistogramAggregationSubCount(String index,
                                                   String startTime,
                                                   String endTime,
                                                   String intervalType,
                                                   String countField) {
        try {
            // 创建一个查询请求，并指定索引名称
            SearchRequest searchRequest = new SearchRequest(index);

            // 按时间范围统计
            DateHistogramAggregationBuilder dateHistogramAggregationBuilder = AggregationBuilders.dateHistogram("dh");
            dateHistogramAggregationBuilder.field("@timestamp");
            switch(intervalType){
                case "day" :
                    dateHistogramAggregationBuilder.calendarInterval(DateHistogramInterval.DAY);
                    dateHistogramAggregationBuilder.format("yyyy-MM-dd");
                    break;
                case "week" :
                    dateHistogramAggregationBuilder.calendarInterval(DateHistogramInterval.WEEK);
                    dateHistogramAggregationBuilder.format("yyyy-MM-dd");
                    break;
                case "month" :
                    dateHistogramAggregationBuilder.calendarInterval(DateHistogramInterval.MONTH);
                    dateHistogramAggregationBuilder.format("yyyy-MM");
                    break;
                case "year" :
                    dateHistogramAggregationBuilder.calendarInterval(DateHistogramInterval.YEAR);
                    dateHistogramAggregationBuilder.format("yyyy");
                    break;
            }
            dateHistogramAggregationBuilder.timeZone(ZoneId.of("Asia/Shanghai"));
            dateHistogramAggregationBuilder.extendedBounds(
                    new LongBounds(
                            DateUtil.parse(startTime).getTime(),
                            DateUtil.parse(endTime).getTime()
                    ));
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            dateHistogramAggregationBuilder.subAggregation(AggregationBuilders.count("count").field(countField));
            searchSourceBuilder.aggregation(dateHistogramAggregationBuilder);
            searchRequest.source(searchSourceBuilder);

            // 发起请求，获取响应结果
            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            // 获取聚合的结果
            Histogram buckets = response.getAggregations().get("dh");
            Bucket result = new Bucket();
            List<String> datas = new ArrayList<>();
            List<String> values = new ArrayList<>();
            // 循环遍历各个桶结果
            for (Histogram.Bucket bucket : buckets.getBuckets()) {
                ValueCount valueCount = bucket.getAggregations().get("count");
                datas.add(bucket.getKeyAsString());
                values.add(String.valueOf(valueCount.getValue()));
            }
            result.setDates(datas);
            result.setValues(values);

            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


}

