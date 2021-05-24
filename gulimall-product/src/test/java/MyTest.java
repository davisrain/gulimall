
import com.dzy.gulimall.product.entity.BrandEntity;
import com.dzy.gulimall.product.service.BrandService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {com.dzy.gulimall.product.GulimallProductMain.class})
public class MyTest {

    @Autowired
    BrandService brandService;

    @Test
    public void test(){
        System.out.println(brandService);
    }

}
