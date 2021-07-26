package com.dzy.gulimall.feign;

import com.dzy.common.utils.R;
import com.dzy.gulimall.to.WeiboAccessTokenTo;
import com.dzy.gulimall.vo.UserLoginVo;
import com.dzy.gulimall.vo.UserRegisterVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("gulimall-member")
public interface MemberFeignService {

    @PostMapping("/member/member/register")
    R register(@RequestBody UserRegisterVo userRegisterVo);

    @PostMapping("/member/member/login")
    R login(@RequestBody UserLoginVo userLoginVo);

    @PostMapping("/member/member/weibo/login")
    R login(@RequestBody WeiboAccessTokenTo weiboAccessTokenTo);
}
