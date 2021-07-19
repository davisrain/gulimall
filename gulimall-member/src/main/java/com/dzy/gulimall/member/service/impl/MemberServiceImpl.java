package com.dzy.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dzy.gulimall.member.entity.MemberLevelEntity;
import com.dzy.gulimall.member.exception.PhoneExistException;
import com.dzy.gulimall.member.exception.UsernameExistException;
import com.dzy.gulimall.member.service.MemberLevelService;
import com.dzy.gulimall.member.to.WeiboAccessTokenTo;
import com.dzy.gulimall.member.vo.MemberLoginVo;
import com.dzy.gulimall.member.vo.MemberRegisterVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzy.common.utils.PageUtils;
import com.dzy.common.utils.Query;

import com.dzy.gulimall.member.dao.MemberDao;
import com.dzy.gulimall.member.entity.MemberEntity;
import com.dzy.gulimall.member.service.MemberService;
import org.springframework.web.client.RestTemplate;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    MemberLevelService memberLevelService;

    @Autowired
    RestTemplate restTemplate;

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

    @Override
    public MemberEntity login(MemberLoginVo memberLoginVo) {
        String account = memberLoginVo.getAccount();
        String password = memberLoginVo.getPassword();
        MemberEntity member = baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("username", account).or().eq("mobile", account));
        if(member == null)
            return null;
        String dbPassword = member.getPassword();
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        boolean matches = passwordEncoder.matches(password, dbPassword);
        if(matches)
            return member;
        else
            return null;
    }

    @Override
    public MemberEntity login(WeiboAccessTokenTo weiboAccessTokenTo) {
        //1.判断数据库里是否已经有weibo uid对应的用户
        MemberEntity weiboMember = baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("weibo_uid", weiboAccessTokenTo.getUid()));
        //2.如果没有，调用微博接口查询用户信息，并进行注册
        if(weiboMember == null) {
            weiboMember = new MemberEntity();
            //调用微博接口使用accessToken查询用户信息https://api.weibo.com/2/users/show.json?uid={0}&access_token={1}
            String url = "https://api.weibo.com/2/users/show.json?uid=" + weiboAccessTokenTo.getUid() + "&access_token=" + weiboAccessTokenTo.getAccessToken();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if(response.getStatusCode().value() == 200) {
                String respJson = response.getBody().toString();
                JSONObject jsonObject = JSON.parseObject(respJson);
                String name = jsonObject.getString("name");
                String gender = jsonObject.getString("gender");
                weiboMember.setNickname(name);
                weiboMember.setGender("M".equalsIgnoreCase(gender) ? 1 : 0);
            }
            weiboMember.setWeiboAccessToken(weiboAccessTokenTo.getAccessToken());
            weiboMember.setWeiboUid(weiboAccessTokenTo.getUid());
            weiboMember.setWeiboExpires(weiboAccessTokenTo.getExpiresIn());
            weiboMember.setCreateTime(new Date());
            baseMapper.insert(weiboMember);
        }
        //3.如果有，直接返回库里的数据
        else {
            MemberEntity updateMember = new MemberEntity();
            updateMember.setId(weiboMember.getId());
            updateMember.setWeiboAccessToken(weiboAccessTokenTo.getAccessToken());
            updateMember.setWeiboUid(weiboAccessTokenTo.getUid());
            updateMember.setWeiboExpires(weiboAccessTokenTo.getExpiresIn());

            weiboMember.setWeiboAccessToken(weiboAccessTokenTo.getAccessToken());
            weiboMember.setWeiboExpires(weiboAccessTokenTo.getExpiresIn());
            baseMapper.updateById(updateMember);
        }
        return weiboMember;
    }

}