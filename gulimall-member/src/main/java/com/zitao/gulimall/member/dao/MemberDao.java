package com.zitao.gulimall.member.dao;

import com.zitao.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author Zitao Wang
 * @email zitao.wang@tum.de
 * @date 2022-04-27 14:38:08
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
