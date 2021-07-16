import com.aliyun.oss.OSSClient;
import com.dzy.gulimall.thirdparty.Component.SmsComponent;
import com.dzy.gulimall.thirdparty.GulimallThirdpartyMain;
import com.dzy.gulimall.thirdparty.utils.HttpUtils;
import org.apache.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest(classes = {com.dzy.gulimall.thirdparty.GulimallThirdpartyMain.class})
public class MyTest {

    @Autowired
    OSSClient ossClient;

    @Autowired
    SmsComponent smsComponent;

    @Test
    public void testSendSmsBySmsComponent() {
        smsComponent.sendSms("17888840358","159784");
    }

    @Test
    public void testSendSms() {
        String host = "https://gyytz.market.alicloudapi.com";
        String path = "/sms/smsSend";
        String method = "POST";
        String appcode = "04c6d255b5ce4e72a0f14980672c1fdf";
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        Map<String, String> querys = new HashMap<String, String>();
        querys.put("mobile", "17888840358");
        querys.put("param", "**code**:12345,**minute**:5");
        querys.put("smsSignId", "2e65b1bb3d054466b82f0c9d125465e2");
        querys.put("templateId", "908e94ccf08b4476ba6c876d13f084ad");
        Map<String, String> bodys = new HashMap<String, String>();


        try {
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            System.out.println(response.toString());
            //获取response的body
            //System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testUpload() throws FileNotFoundException {
        FileInputStream inputStream = new FileInputStream("D:\\movie\\露出照片\\Ek8o-4YUUAEaPYq.jpg");
        ossClient.putObject("gulimall-zhengyudai", "test.jpg", inputStream);
        ossClient.shutdown();
        System.out.println("上传成功");
    }
}
