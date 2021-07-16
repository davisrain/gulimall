package com.dzy.gulimall.controller;

import com.dzy.common.constant.AuthServerConstant;
import com.dzy.common.exception.BizCodeEnum;
import com.dzy.common.utils.R;
import com.dzy.gulimall.Feign.ThirdPartyFeignService;
import com.dzy.gulimall.config.MyWebConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller
public class IndexController {

    /**
     *  页面跳转可以使用SpringMVC提供的viewController进行配置，不用写跳转空方法了
     * @see MyWebConfiguration
     */
//    @GetMapping({"login","/login.html"})
//    public String loginPage() {
//        return "login";
//    }
//
//    @GetMapping({"register", "register.html"})
//    public String registerPage(){
//        return "register";
//    }

    @Autowired
    ThirdPartyFeignService thirdPartyFeignService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone) {
        //TODO 1.接口防刷
        //2.同一号码60s重发限制
        String redisCode = stringRedisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_REDIS_PREFIX + phone);
        if(StringUtils.hasText(redisCode)) {
            long sendTime = Long.parseLong(redisCode.split("_")[1]);
            if(System.currentTimeMillis() - sendTime < 60 * 1000) {
                return R.error(BizCodeEnum.SMS_SENDCODE_EXCEPTION.getCode(), BizCodeEnum.SMS_SENDCODE_EXCEPTION.getMsg());
            }
        }
        String code = UUID.randomUUID().toString().substring(0, 5);
        //3.保存验证码，在登录时进行校验
            //保存到redis，自定义key 为 sms:code: + phoneNumber
            //value存验证码，并且在后面拼接上当前系统时间，这样可以限制同一手机号60秒内不能重发
        stringRedisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_REDIS_PREFIX + phone,
                code + "_" + System.currentTimeMillis(), 5L, TimeUnit.MINUTES);
        thirdPartyFeignService.sendCode(phone, code);
        return R.ok();
    }
}
