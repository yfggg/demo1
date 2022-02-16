package com.example.demo.utils;

import cn.hutool.core.util.StrUtil;
import com.example.demo.entity.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.*;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.range.DateRangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.metrics.ValueCount;
import org.elasticsearch.search.aggregations.metrics.ValueCountAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import springfox.documentation.schema.Entry;

import java.io.IOException;
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
     * @param index 索引
     * @retur
     */
    public boolean createIndex(String index) throws IOException {
        if(isIndexExist(index)){
            log.error(StrUtil.format("索引 {} 已经存在！", index));
            return false;
        }
        CreateIndexRequest request = new CreateIndexRequest(index);
        CreateIndexResponse response = restHighLevelClient.indices().create(request, RequestOptions.DEFAULT);
        return response.isAcknowledged();
    }

    /**
     * 判断索引是否存在
     *
     * @param index
     * @return
     */
    public boolean isIndexExist(final String index) throws IOException {
        GetIndexRequest request = new GetIndexRequest(index);
        return restHighLevelClient.indices().exists(request, RequestOptions.DEFAULT);
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
    public void addData(String index, String id, Map<String, Object> fields) throws IOException {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        for(String key : fields.keySet()){
            builder.field(key, fields.get(key));
        }
        builder.endObject();
        IndexRequest request = new IndexRequest(index).id(id).source(builder);
        restHighLevelClient.index(request, RequestOptions.DEFAULT);
    }


    /**
     * 通过ID 批量更新数据
     *
     * @param fields     要增加的数据
     * @param index      索引，类似数据库
     * @param ids        数据ID列表
     * @return
     */
    public void bulkUpdateDataById(String index, List<String> ids, Map<String, Object> fields) throws IOException {
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
    }

    /**
     * 滚动查询 一般用于数据迁移or索引变更
     *
     * @param index
     * @param size
     * @param consumer
     * @throws IOException
     */
    public void scrollSearch(String index, Integer size, Consumer<SearchHit> consumer) throws IOException {
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
    }

    /**
     * 按年龄层统计
     *
     * @param index
     * @param field
     * @return
     * @throws IOException
     */
    public Bucket statisticsByAge(String index, String field, String subField) throws IOException {
        // 创建一个查询请求，并指定索引名称
        SearchRequest searchRequest = new SearchRequest(index);

        // 按年龄层统计
        DateRangeAggregationBuilder dateRangeAggregationBuilder =
                AggregationBuilders.dateRange("date_range").field(field)
                        .addRange(0, 20).addRange(20, 40).addRange(40, 60)
                        .addRange(60, 80).addRange(80, 100).addRange(100, 120);
        // 上链数统计
        ValueCountAggregationBuilder valueCountAggregationBuilder =
                AggregationBuilders.count("value_count").field(subField);

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        dateRangeAggregationBuilder.subAggregation(valueCountAggregationBuilder);
        searchSourceBuilder.aggregation(dateRangeAggregationBuilder);
        searchRequest.source(searchSourceBuilder);

        //发起请求，获取响应结果
        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        //获取聚合的结果
        Range buckets = response.getAggregations().get("date_range");
        Bucket result = new Bucket();
        List<String> datas = new ArrayList<>();
        List<String> values = new ArrayList<>();
        //循环遍历各个桶结果
        for (Range.Bucket bucket : buckets.getBuckets()) {
            ValueCount valueCount = bucket.getAggregations().get("value_count");
            datas.add(bucket.getKey().toString());
            values.add(String.valueOf(valueCount.getValue()));
        }
        result.setDates(datas);
        result.setValues(values);

        return result;
    }

}

