package com.dzy.gulimall.member.dao;

import com.dzy.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author zhengyu_dai
 * @email zhengyu_dai@foxmail.com
 * @date 2021-05-24 23:14:46
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
