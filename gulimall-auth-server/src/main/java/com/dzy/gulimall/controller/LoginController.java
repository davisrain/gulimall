package com.dzy.gulimall.controller;

import com.alibaba.fastjson.TypeReference;
import com.dzy.common.constant.AuthServerConstant;
import com.dzy.common.exception.BizCodeEnum;
import com.dzy.common.utils.R;
import com.dzy.gulimall.Feign.MemberFeignService;
import com.dzy.gulimall.Feign.ThirdPartyFeignService;
import com.dzy.gulimall.config.MyWebConfiguration;
import com.dzy.gulimall.to.WeiboAccessTokenTo;
import com.dzy.gulimall.vo.UserLoginVo;
import com.dzy.gulimall.vo.UserRegisterVo;
import com.dzy.gulimall.vo.UserRespVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
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

    @Autowired
    RestTemplate restTemplate;

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

    @PostMapping("/login")
    public String login(@Valid UserLoginVo userLoginVo, RedirectAttributes redirectAttributes,
                        HttpSession session) {
        //远程调用登录功能
        R r = memberFeignService.login(userLoginVo);
        if(r.getCode() == 0) {
            UserRespVo user = r.getData("data", new TypeReference<UserRespVo>() {});
            //TODO 1、将cookie的domian设置为父域，使得子域访问的时候也能携带对应的cookie，以便拿到sessionid获取session
            //TODO 2、将session中的序列化方式替换为json，这样就不能每次都使用jdk来反序列化（jdk要求序列化和反序列化的对象要一致）
            session.setAttribute("loginUser", user);
            return "redirect:http://gulimall.com";
        }
        else {
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", r.getMsg());
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }

    @GetMapping("/oauth2/weibo/success")
    public String weiboLogin(@RequestParam("code") String code, RedirectAttributes redirectAttributes,
                             HttpSession session, HttpServletResponse servletResponse) {
        //1.根据code去获取accessToken
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", "YOUR_CLIENT_ID");
        params.add("client_secret", "YOUR_CLIENT_SECRET");
        params.add("grand_type", "authorization_code");
        params.add("redirect_uri", "http://auth.gulimall.com/oauth2/weibo/success");
        params.add("code", code);
        WeiboAccessTokenTo weiboAccessTokenTo = restTemplate.postForObject("https://api.weibo.com/oauth2/access_tocken", params, WeiboAccessTokenTo.class);
        //2.拿到accessToken去调用member远程服务的微博登录功能
        R r = memberFeignService.login(weiboAccessTokenTo);
        //3.根据返回的用户信息决定跳转页面
        if(r.getCode() == 0) {
            UserRespVo user = r.getData("data", new TypeReference<UserRespVo>() {});
            /**
             *  服务器向浏览器发送cookie让其保存的逻辑如下，如果我们可以修改cookie的domain作用域为父域名，
             *  那么就能在访问所有子域的时候带上该cookie
             */
//            Cookie cookie = new Cookie("JSESSIONID", "");
//            cookie.setDomain("");
//            servletResponse.addCookie(cookie);
            session.setAttribute("loginUser", user);
            return "redirect:http://gulimall.com";
        } else {
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", r.getMsg());
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }
}
