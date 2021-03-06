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
 * es ????????????
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
     * ????????????
     *
     * @param index ??????
     * @retur
     */
    public boolean createIndex(String index) {
        if(isIndexExist(index)){
            log.error(StrUtil.format("?????? {} ???????????????", index));
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
     * ????????????????????????
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
     * ????????????
     *
     * @param index
     * @return
     */
    public boolean deleteIndex(String index)  {
        if(!isIndexExist(index)){
            log.error(StrUtil.format("?????? {} ????????????", index));
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
     * ???????????? ??????ID
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
     * ??????ID????????????
     *
     * @param index
     * @param id
     */
    public void deleteDataById(String index, String id) {
        try {
            DeleteRequest request = new DeleteRequest(index, id);
            DeleteResponse response = restHighLevelClient.delete(request, RequestOptions.DEFAULT);
            if(!response.status().equals(RestStatus.OK)) {
                log.warn("?????? _id:{} ???????????????", id);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * ??????ID????????????
     *
     * @param index  ????????????????????????
     * @param id     ??????ID
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
     * ???????????? ????????????????????????or????????????
     *
     * @param index
     * @param size
     * @param consumer
     * @throws IOException
     */
    public void scrollSearch(String index, Integer size, Consumer<SearchHit> consumer) {
        try {
            // ???????????????
            final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1L));
            SearchRequest searchRequest = new SearchRequest(index);
            searchRequest.scroll(scroll);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            // ????????????????????????????????? ???????????????????????????????????????
            searchSourceBuilder.size(size);
            searchRequest.source(searchSourceBuilder);

            // ?????????????????? SearchRequest ??????????????????
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            String scrollId = searchResponse.getScrollId();
            SearchHit[] searchHits = searchResponse.getHits().getHits();
            for (SearchHit searchHit : searchHits) {
                consumer.accept(searchHit);
            }

            // ???????????????????????? Search Scroll api ??????????????????????????? ????????????????????????
            while (searchHits != null && searchHits.length > 0) {
                // ?????????????????? SearchScrollRequest ????????????????????????????????????
                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(scroll);
                searchResponse = restHighLevelClient.scroll(scrollRequest, RequestOptions.DEFAULT);
                scrollId = searchResponse.getScrollId();
                searchHits = searchResponse.getHits().getHits();
                for (SearchHit searchHit : searchHits) {
                    consumer.accept(searchHit);
                }
            }

            // ?????????????????????
            ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
            clearScrollRequest.addScrollId(scrollId);
            ClearScrollResponse clearScrollResponse = restHighLevelClient.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
            boolean succeeded = clearScrollResponse.isSucceeded();
            // ??????????????????
            log.info(String.valueOf(succeeded));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * ????????????
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

            // ????????????
            if (200 == searchResponse.status().getStatus()) {
                // ????????????
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
     * range???????????????
     *
     * @param index
     * @param dateRangeField
     * @return
     * @throws IOException
     */
    public Bucket dateRangeAggregationSubCount(String index, String dateRangeField) {
        try {
            // ????????????????????????????????????????????????
            SearchRequest searchRequest = new SearchRequest(index);

            // ??? dateRangeField ????????????
            DateRangeAggregationBuilder dateRangeAggregationBuilder =
                    AggregationBuilders.dateRange("date_range").field(dateRangeField)
                            .addRange(0, 20).addRange(20, 40).addRange(40, 60)
                            .addRange(60, 80).addRange(80, 100).addRange(100, 120);

            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.aggregation(dateRangeAggregationBuilder);
            searchRequest.source(searchSourceBuilder);

            // ?????????????????????????????????
            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            // ?????????????????????
            Range buckets = response.getAggregations().get("date_range");

            Bucket result = new Bucket();
            List<String> datas = new ArrayList<>();
            List<String> values = new ArrayList<>();

            // ???????????????????????????
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
            log.error("???????????????");
        }
        return null;
    }

    /**
     * term????????????????????????
     *
     * @param index
     * @param termsField
     * @return
     * @throws IOException
     */
    public Bucket termsAggregationSubCount(String index, String termsField) {
        try {
            // ????????????????????????????????????????????????
            SearchRequest searchRequest = new SearchRequest(index);

            // ??? termsField ????????????
            TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms("terms").field(termsField);

            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.aggregation(termsAggregationBuilder);
            searchRequest.source(searchSourceBuilder);

            // ?????????????????????????????????
            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            // ?????????????????????
            Terms buckets = response.getAggregations().get("terms");

            Bucket result = new Bucket();
            List<String> datas = new ArrayList<>();
            List<String> values = new ArrayList<>();

            // ???????????????????????????
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
            log.error("???????????????");
        }
        return null;
    }

    /**
     * ????????????????????????
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
            // ????????????????????????????????????????????????
            SearchRequest searchRequest = new SearchRequest(index);

            // ?????????????????????
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

            // ?????????????????????
            dateHistogramAggregationBuilder.extendedBounds(
                    new ExtendedBounds(
                            DateUtil.parse(startTime).getTime(), DateUtil.parse(endTime).getTime()
                    ));

            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);
            searchSourceBuilder.aggregation(dateHistogramAggregationBuilder);
            searchRequest.source(searchSourceBuilder);

            // ?????????????????????????????????
            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            // ?????????????????????
            Histogram buckets = response.getAggregations().get("dh");

            Bucket result = new Bucket();
            List<String> datas = new ArrayList<>();
            List<String> values = new ArrayList<>();
            List<String> chainGrowthRate = new ArrayList<>();

            // ???????????????????????????
            int thisIssue = 1;
            int lastIssue;
            for (Histogram.Bucket bucket : buckets.getBuckets()) {
                datas.add(bucket.getKeyAsString());
                values.add(String.valueOf(bucket.getDocCount()));
                lastIssue = thisIssue;
                thisIssue += new Long(bucket.getDocCount()).intValue();
                // ??????????????? = (????????? - ?????????) / ????????? ?? 100%
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
            log.error("???????????????");
        } catch (ElasticsearchStatusException e) {
            log.error("??????????????????????????????");
        }
        return null;
    }

}


