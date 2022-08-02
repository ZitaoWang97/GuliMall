package com.zitao.gulimall.search;

import com.alibaba.fastjson.JSON;
import com.zitao.gulimall.search.config.GulimallElasticSearchConfig;
import lombok.Data;
import lombok.ToString;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallSearchApplicationTests {


    @Autowired
    private RestHighLevelClient restHighLevelClient;

    /**
     * 测试存储数据到es
     *
     * @throws IOException
     */
    @Test
    public void contextLoads() throws IOException {
        IndexRequest request = new IndexRequest("user");
        request.id("2");
        User user = new User();
        user.setUsername("nigel wang 2");
        user.setAge(24);
        user.setGender("男");
        String jsonString = JSON.toJSONString(user);
        IndexRequest source = request.source(jsonString, XContentType.JSON);
        IndexResponse indexResponse = restHighLevelClient.index(source, GulimallElasticSearchConfig.COMMON_OPTIONS);
        System.out.println(indexResponse);
    }


    /**
     * 测试检索数据
     *
     * @throws IOException
     */
    @Test
    public void testSearch() throws IOException {
        // 1. 创建检索请求
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("bank");
        // 2. 指定DSL 检索条件语句
        SearchSourceBuilder builder = new SearchSourceBuilder();
        // 2.1 全文检索
        builder.query(QueryBuilders.matchQuery("address", "mill"));
        // 2.2 聚合条件
        TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age");
        builder.aggregation(ageAgg);
        AvgAggregationBuilder ageAvg = AggregationBuilders.avg("ageAvg").field("age");
        builder.aggregation(ageAvg);
        AvgAggregationBuilder balanceAvg = AggregationBuilders.avg("balanceAvg").field("balance");
        builder.aggregation(balanceAvg);
        searchRequest.source(builder);
        // 3. 检索
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest,
                GulimallElasticSearchConfig.COMMON_OPTIONS);
        System.out.println(searchResponse.toString());
        // 4. 分析结果
        SearchHits hits = searchResponse.getHits();
        SearchHit[] hits1 = hits.getHits();
        for (SearchHit documentFields : hits1) {
            String sourceAsString = documentFields.getSourceAsString();
            System.out.println(sourceAsString);
            // 映射为java bean的对象
            Account account = JSON.parseObject(sourceAsString, Account.class);
            System.out.println(account);
        }

        Aggregations aggregations = searchResponse.getAggregations();
        Terms ageAgg1 = aggregations.get("ageAgg");
        for (Terms.Bucket bucket : ageAgg1.getBuckets()) {
            String keyAsString = bucket.getKeyAsString();
            System.out.println(keyAsString + "---" + bucket.getDocCount());
        }

        Avg ageAvg1 = aggregations.get("ageAvg");
        System.out.println("ageAvg:" + ageAvg1.getValue());

        Avg balanceAvg1 = aggregations.get("balanceAvg");
        System.out.println("balanceAvg:" + balanceAvg1.getValue());
    }

    @Data
    class User {
        private int age;
        private String username;
        private String gender;
    }

    @ToString
    @Data
    static class Account {
        private int account_number;
        private int balance;
        private String firstname;
        private String lastname;
        private int age;
        private String gender;
        private String address;
        private String employer;
        private String email;
        private String city;
        private String state;
    }

}
