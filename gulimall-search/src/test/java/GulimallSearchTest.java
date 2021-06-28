
import com.alibaba.fastjson.JSON;
import com.dzy.gulimall.search.config.ElasticSearchConfig;
import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.GsonTester;

import java.io.IOException;

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
    @Data
    class User {
        private String userName;
        private String gender;
        private Integer age;
    }

}
