package com.zitao.gulimall.ware.service.impl;

import com.zitao.common.constant.WareConstant;
import com.zitao.gulimall.ware.entity.PurchaseDetailEntity;
import com.zitao.gulimall.ware.service.PurchaseDetailService;
import com.zitao.gulimall.ware.service.WareSkuService;
import com.zitao.gulimall.ware.vo.MergeVo;
import com.zitao.gulimall.ware.vo.PurchaseDoneItemVo;
import com.zitao.gulimall.ware.vo.PurchaseDoneVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zitao.common.utils.PageUtils;
import com.zitao.common.utils.Query;

import com.zitao.gulimall.ware.dao.PurchaseDao;
import com.zitao.gulimall.ware.entity.PurchaseEntity;
import com.zitao.gulimall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    PurchaseDetailService purchaseDetailService;

    @Autowired
    WareSkuService wareSkuService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils listUnreceive(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(new Query<PurchaseEntity>().getPage(params),
                // 刚新建或者尚未领取的采购单
                new QueryWrapper<PurchaseEntity>().eq("status", 0)
                        .or().eq("status", 1));
        return new PageUtils(page);
    }

    /**
     * 合并采购单
     * @param mergeVo
     * purchaseId: 1, //整单id
     * items:[1,2,3,4] //合并项集合
     */
    @Transactional
    @Override
    public void mergePurchaseDetail(MergeVo mergeVo) {
        Long purchaseId = mergeVo.getPurchaseId();
        // 如果没有指定采购单，则需要新建
        if (purchaseId == null) {
            PurchaseEntity entity = new PurchaseEntity();
            entity.setCreateTime(new Date());
            entity.setUpdateTime(new Date());
            // 默认状态是新建 0
            entity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getCode());
            this.save(entity);
            purchaseId = entity.getId();
        }
        // TODO 确认采购单状态是0，1才可以合并
        // 修改采购需求状态为已分配
        List<Long> items = mergeVo.getItems();
        Long finalPurchaseId = purchaseId;
        List<PurchaseDetailEntity> collect = items.stream().map(item -> {
            PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
            purchaseDetailEntity.setId(item);
            purchaseDetailEntity.setPurchaseId(finalPurchaseId);
            purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());
            return purchaseDetailEntity;
        }).collect(Collectors.toList());
        purchaseDetailService.updateBatchById(collect);
        // 更新采购单updatetime
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(purchaseId);
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);
    }

    @Transactional
    @Override
    public void ReceivedPurchase(List<Long> ids) {
        //1.更新采购单状态及更新时间
        List<PurchaseEntity> collect = ids.stream().map(id -> {
            // 1.1 根据采购单id获取采购单对象
            PurchaseEntity byId = this.getById(id);
            return byId;
        }).filter(entity -> {
            // 1.2确认当前采购单的状态是新建或者已分配的状态
            return entity.getStatus() == WareConstant.PurchaseStatusEnum.CREATED.getCode()
                    || entity.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode();
        }).map(entity -> {
            // 1.3 更新状态和时间
            entity.setStatus(WareConstant.PurchaseStatusEnum.RECEIVE.getCode());
            entity.setUpdateTime(new Date());
            return entity;
        }).collect(Collectors.toList());
        // 1.4 批量修改
        this.updateBatchById(collect);

        // 2. 更新采购单对应采购需求状态
        collect.forEach(entity -> {
            // 2.1 按照采购单id获取对应的采购需求list
            List<PurchaseDetailEntity> detailEntities = purchaseDetailService.list(
                    new QueryWrapper<PurchaseDetailEntity>().eq("purchase_id", entity.getId()));
            // 2.2 更新状态为buying
            List<PurchaseDetailEntity> purchaseDetailEntities = detailEntities.stream().map(detail -> {
                detail.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());
                return detail;
            }).collect(Collectors.toList());
            // 2.3 批量修改
            purchaseDetailService.updateBatchById(purchaseDetailEntities);
        });
    }


    @Transactional
    @Override
    public void finishPurchase(PurchaseDoneVo purchaseDoneVo) {
        //修改采购需求状态
        List<PurchaseDoneItemVo> items = purchaseDoneVo.getItems();
        boolean flag = true;
        List<PurchaseDetailEntity> purchaseDetailEntities = new ArrayList<>();
        for (PurchaseDoneItemVo item : items) {
            PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
            if (item.getStatus() == WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode()) {
                flag = false;
                detailEntity.setStatus(item.getStatus());
            } else {
                detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.FINISH.getCode());
                //增加库存
                PurchaseDetailEntity byId = purchaseDetailService.getById(item.getItemId());
                wareSkuService.addStock(byId.getSkuId(), byId.getWareId(), byId.getSkuNum());
            }
            detailEntity.setId(item.getItemId());
            purchaseDetailEntities.add(detailEntity);
        }
        purchaseDetailService.updateBatchById(purchaseDetailEntities);

        // 更新采购单状态
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(purchaseDoneVo.getId());
        purchaseEntity.setUpdateTime(new Date());
        if (flag) {
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.FINISH.getCode());
        } else {
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.HASERROR.getCode());
        }
        this.updateById(purchaseEntity);
    }

}