package com.zitao.gulimall.coupon.service.impl;

import com.zitao.gulimall.coupon.entity.SeckillSkuRelationEntity;
import com.zitao.gulimall.coupon.service.SeckillSkuRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zitao.common.utils.PageUtils;
import com.zitao.common.utils.Query;

import com.zitao.gulimall.coupon.dao.SeckillSessionDao;
import com.zitao.gulimall.coupon.entity.SeckillSessionEntity;
import com.zitao.gulimall.coupon.service.SeckillSessionService;


@Service("seckillSessionService")
public class SeckillSessionServiceImpl extends ServiceImpl<SeckillSessionDao, SeckillSessionEntity> implements SeckillSessionService {

    @Autowired
    private SeckillSkuRelationService seckillSkuRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SeckillSessionEntity> page = this.page(
                new Query<SeckillSessionEntity>().getPage(params),
                new QueryWrapper<SeckillSessionEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 获取近三天的秒杀活动信息
     *
     * @return
     */
    @Override
    public List<SeckillSessionEntity> getSeckillSessionsIn3Days() {
        QueryWrapper<SeckillSessionEntity> queryWrapper = new QueryWrapper<SeckillSessionEntity>()
                .between("start_time", getStartTime(), getEndTime());
        // 1. 获取到秒杀活动id
        List<SeckillSessionEntity> seckillSessionEntities = this.list(queryWrapper);
        List<SeckillSessionEntity> list = seckillSessionEntities.stream().map(session -> {
            // 2. 查询到对应秒杀活动里的所有商品信息 并且封装到SeckillSessionEntity中
            List<SeckillSkuRelationEntity> skuRelationEntities = seckillSkuRelationService.list(
                    new QueryWrapper<SeckillSkuRelationEntity>().eq("promotion_session_id", session.getId()));
            session.setRelations(skuRelationEntities);
            return session;
        }).collect(Collectors.toList());

        return list;
    }

    /**
     * 获取到当前时间 00:00:00
     *
     * @return
     */
    private String getStartTime() {
        LocalDate now = LocalDate.now();
        LocalDateTime time = now.atTime(LocalTime.MIN);
        String format = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return format;
    }

    /**
     * 获取结束时间  当前天数+2 23:59:59
     *
     * @return
     */
    private String getEndTime() {
        LocalDate now = LocalDate.now();
        LocalDateTime time = now.plusDays(2).atTime(LocalTime.MAX);
        String format = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return format;
    }

}