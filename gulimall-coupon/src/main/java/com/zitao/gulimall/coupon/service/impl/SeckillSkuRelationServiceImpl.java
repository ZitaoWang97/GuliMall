package com.zitao.gulimall.coupon.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zitao.common.utils.PageUtils;
import com.zitao.common.utils.Query;

import com.zitao.gulimall.coupon.dao.SeckillSkuRelationDao;
import com.zitao.gulimall.coupon.entity.SeckillSkuRelationEntity;
import com.zitao.gulimall.coupon.service.SeckillSkuRelationService;
import org.springframework.util.StringUtils;


@Service("seckillSkuRelationService")
public class SeckillSkuRelationServiceImpl extends ServiceImpl<SeckillSkuRelationDao, SeckillSkuRelationEntity> implements SeckillSkuRelationService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<SeckillSkuRelationEntity> wrapper = new QueryWrapper<>();
        String sessionId = (String) params.get("promotionSessionId");
        if (!StringUtils.isEmpty(sessionId)) {
            wrapper.eq("promotion_session_id",sessionId);
        }
        IPage<SeckillSkuRelationEntity> page = this.page(
                new Query<SeckillSkuRelationEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

}