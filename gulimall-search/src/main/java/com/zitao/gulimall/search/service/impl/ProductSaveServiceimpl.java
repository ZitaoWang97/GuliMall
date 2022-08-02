package com.zitao.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;

import com.zitao.common.to.es.SkuEsModel;
import com.zitao.gulimall.search.config.GulimallElasticSearchConfig;
import com.zitao.gulimall.search.constant.EsConstant;
import com.zitao.gulimall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service("ProductSaveService")
@Slf4j
public class ProductSaveServiceimpl implements ProductSaveService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    /**
     * 商品上架 保存数据到ES中
     *
     * @param skuEsModels
     * @return
     * @throws IOException
     */
    @Override
    public boolean saveProductAsIndices(List<SkuEsModel> skuEsModels) throws IOException {
        BulkRequest bulkRequest = new BulkRequest();
        for (SkuEsModel skuEsModel : skuEsModels) {
            // 构造保存请求
            IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);
            indexRequest.id(skuEsModel.getSkuId().toString());
            String s = JSON.toJSONString(skuEsModel);
            indexRequest.source(s, XContentType.JSON);
            bulkRequest.add(indexRequest);
        }
        // 给ES中批量保存数据
        BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest,
                GulimallElasticSearchConfig.COMMON_OPTIONS);
        boolean hasFailures = bulkResponse.hasFailures();
        List<String> collect = Arrays.asList(bulkResponse.getItems()).stream().map(item -> {
            return item.getId();
        }).collect(Collectors.toList());
        if (hasFailures) {
            log.info("商品上架有失败：{}", collect);
        } else {
            log.info("商品上架完成：{}", collect);
        }
        return !hasFailures;
    }
}
