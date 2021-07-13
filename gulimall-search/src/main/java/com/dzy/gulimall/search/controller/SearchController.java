package com.dzy.gulimall.search.controller;


import com.dzy.gulimall.search.service.MallSearchService;
import com.dzy.gulimall.search.vo.SearchParam;
import com.dzy.gulimall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class SearchController {

    @Autowired
    MallSearchService mallSearchService;

    @GetMapping("/list.html")
    public String list(SearchParam searchParam, Model model, HttpServletRequest request) {
        String queryString = request.getQueryString();
        searchParam.set_queryString(queryString);
        SearchResult result =  mallSearchService.search(searchParam);
        model.addAttribute("result", result);
        return "list";
    }
}
