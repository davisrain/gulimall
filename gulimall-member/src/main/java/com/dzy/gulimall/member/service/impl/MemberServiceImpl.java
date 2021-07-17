package com.dzy.gulimall.member.service.impl;

import com.dzy.gulimall.member.entity.MemberLevelEntity;
import com.dzy.gulimall.member.exception.PhoneExistException;
import com.dzy.gulimall.member.exception.UsernameExistException;
import com.dzy.gulimall.member.service.MemberLevelService;
import com.dzy.gulimall.member.vo.MemberRegisterVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzy.common.utils.PageUtils;
import com.dzy.common.utils.Query;

import com.dzy.gulimall.member.dao.MemberDao;
import com.dzy.gulimall.member.entity.MemberEntity;
import com.dzy.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    MemberLevelService memberLevelService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void register(MemberRegisterVo memberRegisterVo) throws PhoneExistException,UsernameExistException{
        //检查账号和手机号是否唯一
        checkUsernameUnique(memberRegisterVo.getUserName());
        checkPhoneUnique(memberRegisterVo.getPhone());
        //获取默认的会员等级
        MemberLevelEntity defaultMemberLevel = memberLevelService.getOne(new QueryWrapper<MemberLevelEntity>().eq("default_status", 1));
        //密码加密存储
        //使用spring提供的BcryptPasswordEncoder自动加盐MD5
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(memberRegisterVo.getPassword());
        //将数据转入member对象并入库
        MemberEntity member = new MemberEntity();
        member.setLevelId(defaultMemberLevel.getId());
        member.setUsername(memberRegisterVo.getUserName());
        member.setMobile(memberRegisterVo.getPhone());
        member.setPassword(encode);
        baseMapper.insert(member);

    }

    public void checkPhoneUnique(String phone) throws PhoneExistException {
        Integer count = baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if(count > 0) {
            throw new PhoneExistException();
        }
    }

    public void checkUsernameUnique(String userName) throws UsernameExistException {
        Integer count = baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", userName));
        if(count > 0) {
            throw new UsernameExistException();
        }
    }

}