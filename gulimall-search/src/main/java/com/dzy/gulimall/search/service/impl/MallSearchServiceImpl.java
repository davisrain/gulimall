package com.dzy.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.dzy.common.to.es.SkuEsModel;
import com.dzy.gulimall.search.config.ElasticSearchConfig;
import com.dzy.gulimall.search.constant.EsConstant;
import com.dzy.gulimall.search.service.MallSearchService;
import com.dzy.gulimall.search.vo.SearchParam;
import com.dzy.gulimall.search.vo.SearchResult;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregator;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service("mallSearchService")
public class MallSearchServiceImpl implements MallSearchService {

    @Autowired
    RestHighLevelClient client;

    @Override
    public SearchResult search(SearchParam searchParam) {
        //1.将searchParam封装为SearchRequest
        SearchRequest searchRequest = prepareSearchRequest(searchParam);
        //2.将searchRequest发送给es,返回searchResponse
        SearchResponse searchResponse = null;
        try {
            searchResponse = client.search(searchRequest, ElasticSearchConfig.COMMON_OPTIONS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //3.将es返回的查询结果SearchResponse封装为searchResult返回
        SearchResult searchResult = handleSearchResponse(searchResponse, searchParam);
        return searchResult;
    }


    private SearchRequest prepareSearchRequest(SearchParam searchParam) {
        SearchSourceBuilder source = new SearchSourceBuilder();
        //1.DSL语句查询部分
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //keyword
        if(StringUtils.hasText(searchParam.getKeyword()))
            boolQueryBuilder.must(QueryBuilders.matchQuery("skuTitle", searchParam.getKeyword()));
        //catalog3Id
        if(searchParam.getCatalog3Id() != null)
            boolQueryBuilder.filter(QueryBuilders.termQuery("catalogId", searchParam.getCatalog3Id()));
        //brandId
        if(searchParam.getBrandId() != null && searchParam.getBrandId().size() > 0)
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", searchParam.getBrandId()));
        //hasStock
        if(searchParam.getHasStock() != null)
            boolQueryBuilder.filter(QueryBuilders.termQuery("hasStock",searchParam.getHasStock() == 1));
        //skuPrice  skuPrice=0_500/_500/500_
        if(StringUtils.hasText(searchParam.getSkuPrice())) {
            String priceRange = searchParam.getSkuPrice();
            String[] priceRanges = priceRange.split("_");
            RangeQueryBuilder skuPrice = QueryBuilders.rangeQuery("skuPrice");
            if(priceRanges.length == 2) {
                if(priceRange.startsWith("_")) {
                    skuPrice.lt(priceRanges[1]);
                } else {
                    skuPrice.gt(priceRanges[0]);
                    skuPrice.lt(priceRanges[1]);
                }
            } else if (priceRanges.length == 1 && priceRange.endsWith("_"))
                skuPrice.gt(priceRanges[0]);
            boolQueryBuilder.filter(skuPrice);
        }
        //attrs 需要使用嵌套查询 attr = 2_5寸:6寸
        if(searchParam.getAttrs() != null && searchParam.getAttrs().size() > 0) {
            for (String attr : searchParam.getAttrs()) {
                BoolQueryBuilder attrQuery = QueryBuilders.boolQuery();
                String attrId = attr.split("_")[0];
                String[] attrValues = attr.split("_")[1].split(":");
                attrQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                attrQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                //nested嵌套查询的builder
                NestedQueryBuilder nestedQueryBuilder = QueryBuilders.nestedQuery("attrs", attrQuery, ScoreMode.None);
                boolQueryBuilder.filter(nestedQueryBuilder);
            }
        }
        source.query(boolQueryBuilder);
        //2.排序、分页、高亮部分
        //排序
        if(StringUtils.hasText(searchParam.getSort())) {
            String[] sort = searchParam.getSort().split("_");
            source.sort(sort[0], "asc".equalsIgnoreCase(sort[1])? SortOrder.ASC : SortOrder.DESC);
        }
        //高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("skuTitle");
        highlightBuilder.preTags("<b style='color:red'>");
        highlightBuilder.postTags("</b>");
        source.highlighter(highlightBuilder);
        //分页
        source.from((searchParam.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE);
        source.size(EsConstant.PRODUCT_PAGESIZE);
        //3.聚合部分
        //品牌信息聚合
        TermsAggregationBuilder brandAgg = AggregationBuilders.terms("brand_agg").field("brandId").size(50);
        brandAgg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brandAgg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        source.aggregation(brandAgg);
        //分类信息聚合
        TermsAggregationBuilder catalogAgg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(50);
        catalogAgg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        source.aggregation(catalogAgg);
        //属性信息聚合 需要采用嵌套聚合
        NestedAggregationBuilder attrAgg = AggregationBuilders.nested("attr_agg", "attrs");
        TermsAggregationBuilder attrIdAgg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId").size(50);
        attrIdAgg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        attrIdAgg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        attrAgg.subAggregation(attrIdAgg);
        source.aggregation(attrAgg);
        System.out.println(source.toString());
        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, source);
        return searchRequest;

    }

    private SearchResult handleSearchResponse(SearchResponse searchResponse, SearchParam searchParam) {
        SearchResult searchResult = new SearchResult();
        //1.查询到的商品信息
        SearchHit[] hits = searchResponse.getHits().getHits();
        List<SkuEsModel> products = new ArrayList<>();
        if(hits != null && hits.length > 0) {
            for (SearchHit hit : hits) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel skuEsModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                products.add(skuEsModel);
            }
        }
        searchResult.setProducts(products);
        //2.查询到分页信息
        //当前页码
        searchResult.setPageNum(searchParam.getPageNum());
        //总记录数
        searchResult.setTotal((int)searchResponse.getHits().getTotalHits().value);
        //总页数
        searchResult.setTotalPages(searchResult.getTotal() % EsConstant.PRODUCT_PAGESIZE == 0 ?
                searchResult.getTotal() / EsConstant.PRODUCT_PAGESIZE : searchResult.getTotal() / EsConstant.PRODUCT_PAGESIZE + 1);
        //3.查询到的聚合信息
        Aggregations aggregations = searchResponse.getAggregations();
        //品牌信息
        Terms brandAgg = aggregations.get("brand_agg");
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        for (Terms.Bucket bucket : brandAgg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            //设置品牌id
            brandVo.setBrandId((Long) bucket.getKey());
            //设置品牌名字
            Terms brandNameAgg = bucket.getAggregations().get("brand_name_agg");
            brandVo.setBrandName(brandNameAgg.getBuckets().get(0).getKeyAsString());
            //设置品牌图片地址
            Terms brandImgAgg = bucket.getAggregations().get("brand_img_agg");
            brandVo.setBrandImg(brandImgAgg.getBuckets().get(0).getKeyAsString());
            brandVos.add(brandVo);
        }
        searchResult.setBrands(brandVos);
        //分类信息
        Terms catalogAgg = aggregations.get("catalog_agg");
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        for (Terms.Bucket bucket : catalogAgg.getBuckets()) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            //设置分类id
            catalogVo.setCatalogId((Long) bucket.getKey());
            //设置分类名字
            Terms catalogNameAgg = bucket.getAggregations().get("catalog_name_agg");
            catalogVo.setCatalogName(catalogNameAgg.getBuckets().get(0).getKeyAsString());
            catalogVos.add(catalogVo);
        }
        searchResult.setCatalogs(catalogVos);
        //属性信息
        Nested attrAgg = aggregations.get("attr_agg");
        Terms attrIdAgg = attrAgg.getAggregations().get("attr_id_agg");
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        for (Terms.Bucket bucket : attrIdAgg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            //设置属性id
            attrVo.setAttrId((Long) bucket.getKey());
            //设置属性名字
            Terms attrNameAgg = bucket.getAggregations().get("attr_name_agg");
            attrVo.setAttrName(attrNameAgg.getBuckets().get(0).getKeyAsString());
            Terms attrValueAgg = bucket.getAggregations().get("attr_value_agg");
            //设置属性的值
            List<String> values = attrValueAgg.getBuckets().stream().map(MultiBucketsAggregation.Bucket::getKeyAsString)
                    .collect(Collectors.toList());
            attrVo.setAttrValue(String.join(",", values));
            attrVos.add(attrVo);
        }
        searchResult.setAttrs(attrVos);
        return searchResult;
    }
}
