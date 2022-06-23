package com.zitao.gulimall.ware.dao;

import com.zitao.gulimall.ware.entity.WareInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 仓库信息
 * 
 * @author Zitao Wang
 * @email zitao.wang@tum.de
 * @date 2022-04-27 14:42:12
 */
@Mapper
public interface WareInfoDao extends BaseMapper<WareInfoEntity> {
	
}
