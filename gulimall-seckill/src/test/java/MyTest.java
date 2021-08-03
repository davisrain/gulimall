import com.dzy.gulimall.seckill.GulimallSeckillMain;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

@SpringBootTest(classes = GulimallSeckillMain.class)
public class MyTest {

    @Test
    public void testLocalDate(){
        System.out.println(LocalDateTime.MAX);
        System.out.println(LocalDateTime.MIN);
    }
}
