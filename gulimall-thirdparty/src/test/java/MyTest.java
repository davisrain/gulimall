import com.aliyun.oss.OSSClient;
import com.dzy.gulimall.thirdparty.GulimallThirdpartyMain;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

@SpringBootTest(classes = {com.dzy.gulimall.thirdparty.GulimallThirdpartyMain.class})
public class MyTest {

    @Autowired
    OSSClient ossClient;

    @Test
    public void testUpload() throws FileNotFoundException {
        FileInputStream inputStream = new FileInputStream("D:\\movie\\露出照片\\Ek8o-4YUUAEaPYq.jpg");
        ossClient.putObject("gulimall-zhengyudai", "test.jpg", inputStream);
        ossClient.shutdown();
        System.out.println("上传成功");
    }
}
