

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dzy.gulimall.product.entity.BrandEntity;
import com.dzy.gulimall.product.service.BrandService;
import com.dzy.gulimall.product.vo.Attr;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

@SpringBootTest(classes = {com.dzy.gulimall.product.GulimallProductMain.class})
public class MyTest {

    @Autowired
    BrandService brandService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    Attr attr;

    @Test
    public void testImport() {
        System.out.println(attr);
    }

    @Test
    public void testRedisson() {
        System.out.println(redissonClient);
    }

    @Test
    public void testRedisTemplate() {
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        opsForValue.set("hello", "world");
        //获取刚才存入redis的数据
        String hello = opsForValue.get("hello");
        System.out.println(hello);

    }

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
