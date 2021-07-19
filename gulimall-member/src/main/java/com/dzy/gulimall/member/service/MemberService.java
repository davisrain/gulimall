package com.dzy.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dzy.common.utils.PageUtils;
import com.dzy.gulimall.member.entity.MemberEntity;
import com.dzy.gulimall.member.exception.PhoneExistException;
import com.dzy.gulimall.member.exception.UsernameExistException;
import com.dzy.gulimall.member.to.WeiboAccessTokenTo;
import com.dzy.gulimall.member.vo.MemberLoginVo;
import com.dzy.gulimall.member.vo.MemberRegisterVo;

import java.util.Map;

/**
 * 会员
 *
 * @author zhengyu_dai
 * @email zhengyu_dai@foxmail.com
 * @date 2021-05-24 23:14:46
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void register(MemberRegisterVo memberRegisterVo) throws PhoneExistException,UsernameExistException;

    void checkPhoneUnique(String phone) throws PhoneExistException;

    void checkUsernameUnique(String userName) throws UsernameExistException;

    MemberEntity login(MemberLoginVo memberLoginVo);

    MemberEntity login(WeiboAccessTokenTo weiboAccessTokenTo);
}

