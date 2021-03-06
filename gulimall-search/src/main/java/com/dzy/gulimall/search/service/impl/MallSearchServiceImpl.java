package com.dzy.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.dzy.common.to.es.SkuEsModel;
import com.dzy.common.utils.R;
import com.dzy.gulimall.search.config.ElasticSearchConfig;
import com.dzy.gulimall.search.constant.EsConstant;
import com.dzy.gulimall.search.feign.ProductFeignService;
import com.dzy.gulimall.search.service.MallSearchService;
import com.dzy.gulimall.search.vo.AttrResponseVo;
import com.dzy.gulimall.search.vo.BrandVo;
import com.dzy.gulimall.search.vo.CategoryVo;
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

@Service("mallSearchService")
public class MallSearchServiceImpl implements MallSearchService {

    @Autowired
    RestHighLevelClient client;

    @Autowired
    ProductFeignService productFeignService;

    @Override
    public SearchResult search(SearchParam searchParam) {
        //1.???searchParam?????????SearchRequest
        SearchRequest searchRequest = prepareSearchRequest(searchParam);
        //2.???searchRequest?????????es,??????searchResponse
        SearchResponse searchResponse = null;
        try {
            searchResponse = client.search(searchRequest, ElasticSearchConfig.COMMON_OPTIONS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //3.???es?????????????????????SearchResponse?????????searchResult??????
        SearchResult searchResult = handleSearchResponse(searchResponse, searchParam);
        return searchResult;
    }


    private SearchRequest prepareSearchRequest(SearchParam searchParam) {
        SearchSourceBuilder source = new SearchSourceBuilder();
        //1.DSL??????????????????
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
        //attrs ???????????????????????? attr = 2_5???:6???
        if(searchParam.getAttrs() != null && searchParam.getAttrs().size() > 0) {
            for (String attr : searchParam.getAttrs()) {
                BoolQueryBuilder attrQuery = QueryBuilders.boolQuery();
                String attrId = attr.split("_")[0];
                String[] attrValues = attr.split("_")[1].split(":");
                attrQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                attrQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                //nested???????????????builder
                NestedQueryBuilder nestedQueryBuilder = QueryBuilders.nestedQuery("attrs", attrQuery, ScoreMode.None);
                boolQueryBuilder.filter(nestedQueryBuilder);
            }
        }
        source.query(boolQueryBuilder);
        //2.??????????????????????????????
        //??????
        if(StringUtils.hasText(searchParam.getSort())) {
            String[] sort = searchParam.getSort().split("_");
            source.sort(sort[0], "asc".equalsIgnoreCase(sort[1])? SortOrder.ASC : SortOrder.DESC);
        }
        //??????
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("skuTitle");
        highlightBuilder.preTags("<b style='color:red'>");
        highlightBuilder.postTags("</b>");
        source.highlighter(highlightBuilder);
        //??????
        source.from((searchParam.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE);
        source.size(EsConstant.PRODUCT_PAGESIZE);
        //3.????????????
        //??????????????????
        TermsAggregationBuilder brandAgg = AggregationBuilders.terms("brand_agg").field("brandId").size(50);
        brandAgg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brandAgg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        source.aggregation(brandAgg);
        //??????????????????
        TermsAggregationBuilder catalogAgg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(50);
        catalogAgg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        source.aggregation(catalogAgg);
        //?????????????????? ????????????????????????
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
        //1.????????????????????????
        SearchHit[] hits = searchResponse.getHits().getHits();
        List<SkuEsModel> products = new ArrayList<>();
        if(hits != null && hits.length > 0) {
            for (SearchHit hit : hits) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel skuEsModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                if(StringUtils.hasText(searchParam.getKeyword())) {
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String highLight = skuTitle.fragments()[0].string();
                    skuEsModel.setSkuTitle(highLight);
                }
                products.add(skuEsModel);
            }
        }
        searchResult.setProducts(products);
        //2.?????????????????????
        //????????????
        searchResult.setPageNum(searchParam.getPageNum());
        //????????????
        searchResult.setTotal((int)searchResponse.getHits().getTotalHits().value);
        //?????????
        searchResult.setTotalPages(searchResult.getTotal() % EsConstant.PRODUCT_PAGESIZE == 0 ?
                searchResult.getTotal() / EsConstant.PRODUCT_PAGESIZE : searchResult.getTotal() / EsConstant.PRODUCT_PAGESIZE + 1);
        //??????????????????
        List<Integer> pageNavs = new ArrayList<>();
        for(int i = 1; i <= searchResult.getTotalPages(); i++){
            pageNavs.add(i);
        }
        searchResult.setPageNavs(pageNavs);
        //3.????????????????????????
        Aggregations aggregations = searchResponse.getAggregations();
        //????????????
        Terms brandAgg = aggregations.get("brand_agg");
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        for (Terms.Bucket bucket : brandAgg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            //????????????id
            brandVo.setBrandId((Long) bucket.getKey());
            //??????????????????
            Terms brandNameAgg = bucket.getAggregations().get("brand_name_agg");
            brandVo.setBrandName(brandNameAgg.getBuckets().get(0).getKeyAsString());
            //????????????????????????
            Terms brandImgAgg = bucket.getAggregations().get("brand_img_agg");
            brandVo.setBrandImg(brandImgAgg.getBuckets().get(0).getKeyAsString());
            brandVos.add(brandVo);
        }
        searchResult.setBrands(brandVos);
        //????????????
        Terms catalogAgg = aggregations.get("catalog_agg");
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        for (Terms.Bucket bucket : catalogAgg.getBuckets()) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            //????????????id
            catalogVo.setCatalogId((Long) bucket.getKey());
            //??????????????????
            Terms catalogNameAgg = bucket.getAggregations().get("catalog_name_agg");
            catalogVo.setCatalogName(catalogNameAgg.getBuckets().get(0).getKeyAsString());
            catalogVos.add(catalogVo);
        }
        searchResult.setCatalogs(catalogVos);
        //????????????
        Nested attrAgg = aggregations.get("attr_agg");
        Terms attrIdAgg = attrAgg.getAggregations().get("attr_id_agg");
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        for (Terms.Bucket bucket : attrIdAgg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            //????????????id
            attrVo.setAttrId((Long) bucket.getKey());
            //??????????????????
            Terms attrNameAgg = bucket.getAggregations().get("attr_name_agg");
            attrVo.setAttrName(attrNameAgg.getBuckets().get(0).getKeyAsString());
            Terms attrValueAgg = bucket.getAggregations().get("attr_value_agg");
            //??????????????????
            List<String> values = attrValueAgg.getBuckets().stream().map(MultiBucketsAggregation.Bucket::getKeyAsString)
                    .collect(Collectors.toList());
            attrVo.setAttrValues(values);
            attrVos.add(attrVo);
        }
        searchResult.setAttrs(attrVos);
        //4.?????????????????????????????????
        //?????????????????????
        List<String> attrs = searchParam.getAttrs();
        List<Long> attrIds = new ArrayList<>();
        if(attrs != null && attrs.size() > 0) {
            List<SearchResult.NavVo> navs = attrs.stream().map(attr -> {
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                String[] s = attr.split("_");
                //?????????????????????id??????attrIds????????????????????????????????????
                attrIds.add(Long.parseLong(s[0]));
                //??????????????????,????????????gulimall-product??????
                R r = productFeignService.getAttrByAttrId(Long.parseLong(s[0]));
                if (r.getCode() == 0) {
                    AttrResponseVo attrResponseVo = r.getData("attr", new TypeReference<AttrResponseVo>() {
                    });
                    navVo.setAttrName(attrResponseVo.getAttrName());
                } else {
                    navVo.setAttrName(s[0]);
                }
                //???????????????
                navVo.setAttrValue(s[1]);
                //???????????????????????????????????????
                String url = replaceQueryUrl(searchParam.get_queryString(), attr, "attrs");
                navVo.setLink(StringUtils.hasText(url) ? "http://search.gulimall.com/list.html?" + url : "http://search.gulimall.com/list.html");
                return navVo;
            }).collect(Collectors.toList());
            searchResult.setNavs(navs);
            searchResult.setAttrIds(attrIds);
        }
        //?????????????????????
        List<Long> brandIds = searchParam.getBrandId();
        if(brandIds != null && brandIds.size() > 0) {
            SearchResult.NavVo navVo = new SearchResult.NavVo();
            //???????????????????????????????????????navs????????????????????????????????????
            List<SearchResult.NavVo> navs = searchResult.getNavs();
            if(navs == null)
                navs = new ArrayList<>();
            navVo.setAttrName("??????");
            //?????????????????????????????????(????????????es?????????????????????????????????????????????????????????)
            String queryString  = searchParam.get_queryString();
            R r = productFeignService.getBrandsByBrandIds(brandIds);
            if(r.getCode() == 0) {
                List<BrandVo> brands = r.getData("brands", new TypeReference<List<BrandVo>>() {});
                StringBuilder sb = new StringBuilder();
                for (BrandVo brand : brands) {
                    sb.append(brand.getName()).append(";");
                    queryString = replaceQueryUrl(queryString, String.valueOf(brand.getBrandId()), "brandId");
                }
                navVo.setAttrValue(sb.substring(0, sb.length() - 1));
            }
            navVo.setLink(StringUtils.hasText(queryString) ? "http://search.gulimall.com/list.html?" + queryString : "http://search.gulimall.com/list.html");
            navs.add(navVo);
            searchResult.setNavs(navs);
        }
        //?????????????????????
        Long catalog3Id = searchParam.getCatalog3Id();
        if (catalog3Id != null) {
            SearchResult.NavVo navVo = new SearchResult.NavVo();
            List<SearchResult.NavVo> navs = searchResult.getNavs();
            if (navs == null)
                navs = new ArrayList<>();
            navVo.setAttrName("??????");
            //??????????????????????????????
            R r = productFeignService.getCategoryByCatId(catalog3Id);
            if (r.getCode() == 0) {
                CategoryVo category = r.getData("category", new TypeReference<CategoryVo>() {
                });
                navVo.setAttrValue(category.getName());
            }
            String queryString = replaceQueryUrl(searchParam.get_queryString(), String.valueOf(catalog3Id), "catalog3Id");
            navVo.setLink(StringUtils.hasText(queryString) ? "http://search.gulimall.com/list.html?" + queryString : "http://search.gulimall.com/list.html");
            navs.add(navVo);
            searchResult.setNavs(navs);
        }
        return searchResult;
    }

    private String replaceQueryUrl(String queryString, String value, String key) {
        String encodeAttr = null;
        try {
            //?????????????????????url????????????????????????????????????????????????java???url????????????????????????+?????????????????????%20???
            //????????????????????????????????????+?????????%20
            encodeAttr = URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return queryString.replaceAll("&?" + key + "=" + encodeAttr, "");
    }
}
