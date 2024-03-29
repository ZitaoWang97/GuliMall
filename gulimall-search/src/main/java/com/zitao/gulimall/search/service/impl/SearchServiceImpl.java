package com.zitao.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import com.zitao.common.to.es.SkuEsModel;
import com.zitao.common.utils.R;
import com.zitao.gulimall.search.config.GulimallElasticSearchConfig;
import com.zitao.gulimall.search.constant.EsConstant;
import com.zitao.gulimall.search.feign.ProductFeignService;
import com.zitao.gulimall.search.service.SearchService;
import com.zitao.gulimall.search.vo.AttrResponseVo;
import com.zitao.gulimall.search.vo.SearchParam;
import com.zitao.gulimall.search.vo.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service("SearchService")
public class SearchServiceImpl implements SearchService {
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private ProductFeignService productFeignService;

    /**
     * 用es进行商品页面的检索
     *
     * @param searchParam
     * @return
     */
    @Override
    public SearchResult getSearchResult(SearchParam searchParam) {
        SearchResult searchResult = null;
        // 1. 准备检索请求
        SearchRequest request = bulidSearchRequest(searchParam);
        try {
            // 2. 执行检索请求
            SearchResponse searchResponse = restHighLevelClient.search(request, GulimallElasticSearchConfig.COMMON_OPTIONS);
            // 3. 分析响应数据封装成需要的格式
            searchResult = bulidSearchResult(searchParam, searchResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return searchResult;
    }

    /**
     * 构建结果数据
     *
     * @param searchParam
     * @param searchResponse
     * @return
     */
    private SearchResult bulidSearchResult(SearchParam searchParam, SearchResponse searchResponse) {
        SearchResult result = new SearchResult();
        SearchHits hits = searchResponse.getHits();
        // 1. 封装查询到的商品信息
        if (hits.getHits() != null && hits.getHits().length > 0) {
            List<SkuEsModel> skuEsModels = new ArrayList<>();
            for (SearchHit hit : hits) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel skuEsModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                // 1.1 设置高亮属性
                if (!StringUtils.isEmpty(searchParam.getKeyword())) {
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String highLight = skuTitle.getFragments()[0].string();
                    skuEsModel.setSkuTitle(highLight);
                }
                skuEsModels.add(skuEsModel);
            }
            result.setProduct(skuEsModels);
        }

        // 2. 封装分页信息
        // 2.1 当前页码
        result.setPageNum(searchParam.getPageNum());
        // 2.2 总记录数
        long total = hits.getTotalHits().value;
        result.setTotal(total);
        // 2.3 总页码
        Integer totalPages = (int) total % EsConstant.PRODUCT_PAGESIZE == 0 ?
                (int) total / EsConstant.PRODUCT_PAGESIZE : (int) total / EsConstant.PRODUCT_PAGESIZE + 1;
        result.setTotalPages(totalPages);
        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);

        // 3. 查询结果涉及到的品牌
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        Aggregations aggregations = searchResponse.getAggregations();
        //ParsedLongTerms用于接收terms聚合的结果，并且可以把key转化为Long类型的数据
        ParsedLongTerms brandAgg = aggregations.get("brandAgg");
        for (Terms.Bucket bucket : brandAgg.getBuckets()) {
            // 3.1 得到品牌id
            Long brandId = bucket.getKeyAsNumber().longValue();
            Aggregations subBrandAggs = bucket.getAggregations();
            // 3.2 得到品牌图片
            ParsedStringTerms brandImgAgg = subBrandAggs.get("brandImgAgg");
            String brandImg = brandImgAgg.getBuckets().get(0).getKeyAsString();
            // 3.3 得到品牌名字
            Terms brandNameAgg = subBrandAggs.get("brandNameAgg");
            String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo(brandId, brandName, brandImg);
            brandVos.add(brandVo);
        }
        result.setBrands(brandVos);

        // 4. 查询涉及到的所有分类
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        ParsedLongTerms catalogAgg = aggregations.get("catalogAgg");
        for (Terms.Bucket bucket : catalogAgg.getBuckets()) {
            // 4.1 获取分类id
            Long catalogId = bucket.getKeyAsNumber().longValue();
            Aggregations subcatalogAggs = bucket.getAggregations();
            // 4.2 获取分类名
            ParsedStringTerms catalogNameAgg = subcatalogAggs.get("catalogNameAgg");
            String catalogName = catalogNameAgg.getBuckets().get(0).getKeyAsString();
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo(catalogId, catalogName);
            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);

        // 5. 查询涉及到的所有属性
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        //ParsedNested用于接收内置属性的聚合
        ParsedNested parsedNested = aggregations.get("attrs");
        ParsedLongTerms attrIdAgg = parsedNested.getAggregations().get("attrIdAgg");
        for (Terms.Bucket bucket : attrIdAgg.getBuckets()) {
            // 5.1 查询属性id
            Long attrId = bucket.getKeyAsNumber().longValue();
            Aggregations subAttrAgg = bucket.getAggregations();
            // 5.2 查询属性名
            ParsedStringTerms attrNameAgg = subAttrAgg.get("attrNameAgg");
            String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
            // 5.3 查询属性值
            ParsedStringTerms attrValueAgg = subAttrAgg.get("attrValueAgg");
            List<String> attrValues = new ArrayList<>();
            for (Terms.Bucket attrValueAggBucket : attrValueAgg.getBuckets()) {
                String attrValue = attrValueAggBucket.getKeyAsString();
                attrValues.add(attrValue);
                List<SearchResult.NavVo> navVos = new ArrayList<>();
            }
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo(attrId, attrName, attrValues);
            attrVos.add(attrVo);
        }
        result.setAttrs(attrVos);

        // 6. 构建面包屑导航
        List<String> attrs = searchParam.getAttrs();
        if (attrs != null && attrs.size() > 0) {
            List<SearchResult.NavVo> navVos = attrs.stream().map(attr -> {
                // 6.1 遍历所有的筛选属性 作用为 attr=8_高通 -> CPU品牌:高通
                String[] split = attr.split("_");
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                // 6.2 设置属性值
                navVo.setNavValue(split[1]);
                // 6.3 根据属性id查询并设置属性名
                try {
                    R r = productFeignService.info(Long.parseLong(split[0]));
                    if (r.getCode() == 0) {
                        AttrResponseVo attrResponseVo = JSON.parseObject(JSON.toJSONString(r.get("attr")),
                                new TypeReference<AttrResponseVo>() {
                                });
                        navVo.setNavName(attrResponseVo.getAttrName());
                    }
                } catch (Exception e) {
                    log.error("远程调用商品服务查询属性失败", e);
                }
                // 6.4 设置面包屑跳转链接
                String queryString = searchParam.get_queryString();
                String encode = null;
                try {
                    encode = URLEncoder.encode(attr, "UTF-8");
                    encode = encode.replace("+", "%20");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                // 6.5 得到取消掉该面包屑后的URL地址
                String replace = queryString.replace("&attrs=" + encode, "")
                        .replace("attrs=" + encode + "&", "")
                        .replace("attrs=" + encode, "");
                navVo.setLink("http://search.gulimall.com/list.html" + (replace.isEmpty() ? "" : "?" + replace));
                return navVo;
            }).collect(Collectors.toList());
            result.setNavs(navVos);
        }
        return result;
    }


    /**
     * 构建检索请求
     *
     * @param searchParam
     * @return
     */
    private SearchRequest bulidSearchRequest(SearchParam searchParam) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        /**
         * 查询：模糊匹配 过滤（按照属性、分类、品牌、价格区间、库存）
         */
        //1. 构建bool query
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //1.1 bool must 模糊匹配
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            boolQueryBuilder.must(QueryBuilders.matchQuery("skuTitle", searchParam.getKeyword()));
        }
        //1.2 bool filter 过滤（按照属性、分类、品牌、价格区间、库存）
        //1.2.1 catalog 三级分类id
        if (searchParam.getCatalog3Id() != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("catalogId", searchParam.getCatalog3Id()));
        }
        //1.2.2 brand 品牌id
        if (searchParam.getBrandId() != null && searchParam.getBrandId().size() > 0) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", searchParam.getBrandId()));
        }
        //1.2.3 hasStock 库存
        if (searchParam.getHasStock() != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("hasStock", searchParam.getHasStock() == 1));
        }
        //1.2.4 priceRange 价格范围 1_500 / _500 / 500_ 三种情况
        if (!StringUtils.isEmpty(searchParam.getSkuPrice())) {
            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("skuPrice");
            String[] prices = searchParam.getSkuPrice().split("_");
            if (prices.length == 1) {
                if (searchParam.getSkuPrice().startsWith("_")) {
                    // _500
                    rangeQueryBuilder.lte(Integer.parseInt(prices[0]));
                } else {
                    // 500_
                    rangeQueryBuilder.gte(Integer.parseInt(prices[0]));
                }
            } else if (prices.length == 2) {
                // 1_500
                if (!prices[0].isEmpty()) {
                    rangeQueryBuilder.gte(Integer.parseInt(prices[0]));
                }
                rangeQueryBuilder.lte(Integer.parseInt(prices[1]));
            }
            boolQueryBuilder.filter(rangeQueryBuilder);
        }
        //1.2.5 attrs-nested 属性
        //attrs=1_5寸:8寸&attrs=2_16G:8G
        List<String> attrs = searchParam.getAttrs();
        if (attrs != null && attrs.size() > 0) {
            attrs.forEach(attr -> {
                // 每一个必须都得生成一个nested查询
                BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
                // attr : "1_5寸:8寸"
                String[] attrSplit = attr.split("_");
                // 检索的属性id
                queryBuilder.must(QueryBuilders.termQuery("attrs.attrId", attrSplit[0]));
                // 检索的属性值
                String[] attrValues = attrSplit[1].split(":");
                queryBuilder.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                NestedQueryBuilder nestedQueryBuilder = QueryBuilders.nestedQuery("attrs", queryBuilder, ScoreMode.None);
                boolQueryBuilder.filter(nestedQueryBuilder);
            });
        }
        //1. bool query构建完成
        searchSourceBuilder.query(boolQueryBuilder);

        //2. sort  sort=saleCount_desc/asc
        if (!StringUtils.isEmpty(searchParam.getSort())) {
            String[] sortSplit = searchParam.getSort().split("_");
            searchSourceBuilder.sort(sortSplit[0],
                    sortSplit[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC);
        }

        //3. 分页
        searchSourceBuilder.from((searchParam.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE);
        searchSourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);

        //4. 高亮highlight
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuTitle");
            highlightBuilder.preTags("<b style='color:red'>");
            highlightBuilder.postTags("</b>");
            searchSourceBuilder.highlighter(highlightBuilder);
        }

        //5. 聚合
        //5.1 按照brand聚合
        TermsAggregationBuilder brandAgg = AggregationBuilders.terms("brandAgg").field("brandId").size(50);
        TermsAggregationBuilder brandNameAgg = AggregationBuilders.terms("brandNameAgg").field("brandName").size(1);
        TermsAggregationBuilder brandImgAgg = AggregationBuilders.terms("brandImgAgg").field("brandImg");
        // 添加子聚合
        brandAgg.subAggregation(brandNameAgg);
        brandAgg.subAggregation(brandImgAgg);
        searchSourceBuilder.aggregation(brandAgg);

        //5.2 按照catalog聚合
        TermsAggregationBuilder catalogAgg = AggregationBuilders.terms("catalogAgg").field("catalogId");
        TermsAggregationBuilder catalogNameAgg = AggregationBuilders.terms("catalogNameAgg").field("catalogName").size(1);
        catalogAgg.subAggregation(catalogNameAgg);
        searchSourceBuilder.aggregation(catalogAgg);

        //5.3 按照attrs聚合
        NestedAggregationBuilder nestedAggregationBuilder = new NestedAggregationBuilder("attrs", "attrs");
        //按照attrId聚合
        TermsAggregationBuilder attrIdAgg = AggregationBuilders.terms("attrIdAgg").field("attrs.attrId");
        //按照attrId聚合之后再按照attrName和attrValue聚合
        TermsAggregationBuilder attrNameAgg = AggregationBuilders.terms("attrNameAgg").field("attrs.attrName");
        TermsAggregationBuilder attrValueAgg = AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue");
        attrIdAgg.subAggregation(attrNameAgg);
        attrIdAgg.subAggregation(attrValueAgg);

        nestedAggregationBuilder.subAggregation(attrIdAgg);
        searchSourceBuilder.aggregation(nestedAggregationBuilder);

        log.debug("构建的DSL语句 {}", searchSourceBuilder.toString());

        SearchRequest request = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, searchSourceBuilder);
        return request;
    }
}
