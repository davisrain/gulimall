package com.dzy.gulimall.search.service;


import com.dzy.common.to.es.SkuEsModel;

import java.util.List;

public interface ProductSaveService {
    boolean upProduct(List<SkuEsModel> skuEsModels);
}
