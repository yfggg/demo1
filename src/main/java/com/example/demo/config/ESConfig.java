package com.example.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.*;
import org.elasticsearch.client.*;
import org.elasticsearch.common.unit.TimeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;

import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Configuration
public class ESConfig {

//    @Bean
//    public RestClientBuilder restClientBuilder() {
//        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
//        credentialsProvider.setCredentials(AuthScope.ANY,
//                new UsernamePasswordCredentials("elastic", "kareza"));
//        return RestClient.builder(
//                new HttpHost("192.168.6.62", 9200, "http"),
//                new HttpHost("192.168.6.63", 9200, "http"),
//                new HttpHost("192.168.6.64", 9200, "http"))
//                .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
//                    @Override
//                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
//                        return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
//                    }
//                });
//    }

    @Bean
    public RestClientBuilder restClientBuilder() {
        return RestClient.builder(
                new HttpHost("127.0.0.1", 9201, "http"));
    }

    @Bean
    public RestHighLevelClient restHighLevelClient(@Autowired RestClientBuilder restClientBuilder) {
        return new RestHighLevelClient(restClientBuilder);
    }

    @Bean
    public BulkProcessor bulkProcessor(@Autowired RestClientBuilder restClientBuilder) {

        BulkProcessor.Listener listener = new BulkProcessor.Listener() {
            @Override
            public void beforeBulk(long executionId, BulkRequest request) {
                log.info("1. ???beforeBulk?????????[{}] ?????? {} ????????????", executionId, request.numberOfActions());
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
                if (!response.hasFailures()) {
                    log.info("2. ???afterBulk-??????????????? [{}] ????????? {} ms", executionId, response.getTook().getMillis());
                } else {
                    BulkItemResponse[] items = response.getItems();
                    for (BulkItemResponse item : items) {
                        if (item.isFailed()) {
                            log.info("2. ???afterBulk-??????????????? [{}] ????????????????????? : {}", executionId, item.getFailureMessage());
                            break;
                        }
                    }
                }
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                List<DocWriteRequest<?>> requests = request.requests();
                List<String> esIds = requests.stream().map(DocWriteRequest::id).collect(Collectors.toList());
                log.error("3. ???afterBulk-failure?????????es??????bluk??????,?????????esId??????{}", esIds, failure);

            }
        };

        return BulkProcessor.builder(
                (request, bulkListener) ->
                        this.restHighLevelClient(restClientBuilder).bulkAsync(request, RequestOptions.DEFAULT, bulkListener), listener)
                //??????10000????????????
//                .setBulkActions(10000)
                //????????????8M?????????
//                .setBulkSize(new ByteSizeValue(8L, ByteSizeUnit.MB))
                //?????????????????????10s
//                .setFlushInterval(TimeValue.timeValueSeconds(10))
                //???????????????????????????????????????
                .setConcurrentRequests(9)
                //??????????????????
                .setBackoffPolicy(BackoffPolicy.constantBackoff(TimeValue.timeValueSeconds(1), 3))
                .build();
    }

}
