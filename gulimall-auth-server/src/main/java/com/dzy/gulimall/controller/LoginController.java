package com.dzy.gulimall.controller;

import com.dzy.common.constant.AuthServerConstant;
import com.dzy.common.exception.BizCodeEnum;
import com.dzy.common.utils.R;
import com.dzy.gulimall.Feign.MemberFeignService;
import com.dzy.gulimall.Feign.ThirdPartyFeignService;
import com.dzy.gulimall.config.MyWebConfiguration;
import com.dzy.gulimall.vo.UserRegisterVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class LoginController {

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

    @Autowired
    MemberFeignService memberFeignService;
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

    /**
     *  redirectAttributes的原理是将数据放到session中，这样页面跳转之后仍然可以拿到数据，
     *  并且下一个页面读取数据之后就会删掉。
     *  //TODO 怎么解决分布式下session数据的问题
     */
    @PostMapping("/register")
    public String register(@Valid UserRegisterVo userRegisterVo, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        //1.如果数据校验出现错误
        Map<String, String> errors = null;
        if(bindingResult.hasErrors()) {
            errors = bindingResult.getFieldErrors().stream().
                    collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/register.html";
        }
        //2.检验验证码是否正确
        String redisCode = stringRedisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_REDIS_PREFIX + userRegisterVo.getPhone());
        if(StringUtils.hasText(redisCode)) {
            //删除redis中存储的验证码
            stringRedisTemplate.delete(AuthServerConstant.SMS_CODE_REDIS_PREFIX + userRegisterVo.getPhone());
            String code = redisCode.split("_")[0];
            if(code.equals(userRegisterVo.getCode())) {
               //3.远程调用注册方法
                R r = memberFeignService.register(userRegisterVo);
                if(r.getCode() == 0) {
                    //4.成功返回登录页
                    return "redirect:http://auth.gulimall.com/login.html";
                } else {
                    errors = new HashMap<>();
                    errors.put("msg", r.getMsg());
                    redirectAttributes.addFlashAttribute("errors", errors);
                    return "redirect:http://auth.gulimall.com/register.html";
                }
            } else {
                errors = new HashMap<>();
                errors.put("code", "验证码错误");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:http://auth.gulimall.com/register.html";
           }
        } else {
            errors = new HashMap<>();
            errors.put("code", "验证码错误");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/register.html";
        }
    }
}
