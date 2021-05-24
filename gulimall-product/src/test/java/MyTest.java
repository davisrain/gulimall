
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
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
    public void testAdd() {
        BrandEntity brandEntity = new BrandEntity();
        brandEntity.setBrandId(1L);
        brandEntity.setName("华为");
        brandService.save(brandEntity);
    }

    @Test
    public void testUpdate() {
        BrandEntity brandEntity = new BrandEntity();
        brandEntity.setBrandId(1L);
        brandEntity.setDescript("华为");
        brandService.updateById(brandEntity);
        System.out.println("更新成功。。");
    }

    @Test
    public void testSelect() {
        BrandEntity brand = brandService.getOne(new QueryWrapper<BrandEntity>().eq("brand_id", 1L));
        System.out.println(brand);
    }
}
