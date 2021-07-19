package com.dzy.gulimall.member.controller;

import java.util.Arrays;
import java.util.Map;

import com.dzy.common.exception.BizCodeEnum;
import com.dzy.gulimall.member.exception.PhoneExistException;
import com.dzy.gulimall.member.exception.UsernameExistException;
import com.dzy.gulimall.member.feign.CouponFeignService;
import com.dzy.gulimall.member.to.WeiboAccessTokenTo;
import com.dzy.gulimall.member.vo.MemberLoginVo;
import com.dzy.gulimall.member.vo.MemberRegisterVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.dzy.gulimall.member.entity.MemberEntity;
import com.dzy.gulimall.member.service.MemberService;
import com.dzy.common.utils.PageUtils;
import com.dzy.common.utils.R;



/**
 * 会员
 *
 * @author zhengyu_dai
 * @email zhengyu_dai@foxmail.com
 * @date 2021-05-24 23:14:46
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    @Autowired
    CouponFeignService couponFeignService;

    @RequestMapping("/coupons")
    public R coupons() {
        MemberEntity member = new MemberEntity();
        member.setNickname("张三");
        R r = couponFeignService.memberCoupons();
        return R.ok().put("member", member).put("coupons", r.get("coupons"));
    }
    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("member:member:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("member:member:info")
    public R info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("member:member:save")
    public R save(@RequestBody MemberEntity member){
		memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("member:member:update")
    public R update(@RequestBody MemberEntity member){
		memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("member:member:delete")
    public R delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

    @PostMapping("/register")
    public R register(@RequestBody MemberRegisterVo memberRegisterVo) {
        try {
            memberService.register(memberRegisterVo);
        } catch (UsernameExistException e) {
            return R.error(BizCodeEnum.USER_EXIST_EXCEPTION);
        } catch (PhoneExistException e) {
            return R.error(BizCodeEnum.PHONE_EXIST_EXCEPTION);
        }
        return R.ok();
    }

    @PostMapping("/login")
    public R login(@RequestBody MemberLoginVo memberLoginVo) {
        MemberEntity member = memberService.login(memberLoginVo);
        if(member != null)
            return R.ok().setData(member);
        else
            return R.error(BizCodeEnum.ACCOUNT_PASSWORD_INVALID_EXCEPTION);
    }

    @PostMapping("/weibo/login")
    public R login(@RequestBody WeiboAccessTokenTo weiboAccessTokenTo) {
        MemberEntity member = memberService.login(weiboAccessTokenTo);
        if(member != null)
            return R.ok().setData(member);
        else
            return R.error(BizCodeEnum.ACCOUNT_PASSWORD_INVALID_EXCEPTION);
    }

}
