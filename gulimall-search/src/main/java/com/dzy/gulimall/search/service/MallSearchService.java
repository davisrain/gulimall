package com.dzy.gulimall.search.service;

import com.dzy.gulimall.search.vo.SearchParam;
import com.dzy.gulimall.search.vo.SearchResult;

public interface MallSearchService {
    SearchResult search(SearchParam searchParam);
}
