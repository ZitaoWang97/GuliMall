package com.zitao.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zitao.common.utils.PageUtils;
import com.zitao.gulimall.member.entity.MemberCollectSpuEntity;

import java.util.Map;

/**
 * 会员收藏的商品
 *
 * @author Zitao Wang
 * @email zitao.wang@tum.de
 * @date 2022-04-27 14:38:08
 */
public interface MemberCollectSpuService extends IService<MemberCollectSpuEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

