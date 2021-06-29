
import com.alibaba.fastjson.JSON;
import com.dzy.gulimall.search.config.ElasticSearchConfig;
import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest(classes = {com.dzy.gulimall.search.GulimallSearchMain.class})
public class GulimallSearchTest {
    @Autowired
    RestHighLevelClient client;
    @Test
    public void testEsRestClient() {
        System.out.println(client);
    }

    /**
     *  新增或修改一个数据
     */
    @Test
    public void indexData() throws IOException {
        IndexRequest request = new IndexRequest("users");
        request.id("1");
        //1.使用key-value对  request.source("userName", "zhangsan", "age", 18, "gender", "男");
        //2.使用map request.source(map);
        //3.使用XContentBuilder
        //4.使用json格式数据
        User user = new User();
        user.setUserName("zhangsan");
        user.setGender("男");
        user.setAge(18);
        String userJson = JSON.toJSONString(user);
        request.source(userJson, XContentType.JSON);    //使用json格式的时候source需要多传一个参数，指定类型
        //执行可分为同步执行和异步执行，以下示例为同步执行
        IndexResponse response = client.index(request, ElasticSearchConfig.COMMON_OPTIONS);
        System.out.println(response);
    }

    /**
     *  复杂查询测试
     */
    @Test
    public void searchData() throws IOException {
        //1.创建查询请求对象
        SearchRequest searchRequest = new SearchRequest();
        //2.指定要查询的索引
        searchRequest.indices("newbank");
        //3.构建查询体对象
        SearchSourceBuilder source = new SearchSourceBuilder();
        source.query(QueryBuilders.matchQuery("address", "mill"));
        source.aggregation(AggregationBuilders.terms("ageGroup").field("age")
                .subAggregation(AggregationBuilders.avg("avgBalance").field("balance")));
//        source.from();
//        source.size();
        System.out.println(source);
        searchRequest.source(source);
        //4.发送查询请求
        SearchResponse response = client.search(searchRequest, ElasticSearchConfig.COMMON_OPTIONS);
        System.out.println(response);
        //5.解析返回对象
        SearchHit[] hits = response.getHits().getHits();
        List<Account> accounts = new ArrayList<>();
        for (SearchHit hit : hits) {
            String sourceAsString = hit.getSourceAsString();
            Account account = JSON.parseObject(sourceAsString, Account.class);
            accounts.add(account);
        }
        System.out.println(accounts);
        Terms ageGroup = response.getAggregations().get("ageGroup");
        for (Terms.Bucket bucket : ageGroup.getBuckets()) {
            Avg avgBalance = bucket.getAggregations().get("avgBalance");
            double balance = avgBalance.getValue();
            System.out.println(bucket.getKey() + " " + bucket.getDocCount() + " " + balance);
        }
    }

    @Data
    static class Account {
        private String accountNumber;
        private BigDecimal balance;
        private String firstname;
        private String lastname;
        private Integer age;
        private String gender;
        private String address;
        private String employer;
        private String email;
        private String city;
        private String state;
    }



    @Data
    class User {
        private String userName;
        private String gender;
        private Integer age;
    }

}
