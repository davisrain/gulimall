package com.dzy.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.dzy.common.to.es.SkuEsModel;
import com.dzy.gulimall.search.config.ElasticSearchConfig;
import com.dzy.gulimall.search.constant.EsConstant;
import com.dzy.gulimall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service("productSaveService")
public class ProductSaveServiceImpl implements ProductSaveService {

    @Autowired
    RestHighLevelClient client;

    @Override
    public boolean upProduct(List<SkuEsModel> skuEsModels) {
        //1.创建索引
        //2.插入sku数据到es
        try {
            BulkRequest bulkRequest = new BulkRequest();
            for (SkuEsModel skuEsModel : skuEsModels) {
                IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);
                indexRequest.id(String.valueOf(skuEsModel.getSkuId()));
                indexRequest.source(JSON.toJSONString(skuEsModel), XContentType.JSON);
                bulkRequest.add(indexRequest);
            }
            BulkResponse response = client.bulk(bulkRequest, ElasticSearchConfig.COMMON_OPTIONS);
            boolean hasFailures = response.hasFailures();
            if(hasFailures) {
                String failureMessage = response.buildFailureMessage();
                System.out.println(failureMessage);
            }
            return !hasFailures;
        } catch (Exception e) {
            log.error("商品上架异常，原因是：{}", e.getMessage());
            return false;
        }
    }
}
