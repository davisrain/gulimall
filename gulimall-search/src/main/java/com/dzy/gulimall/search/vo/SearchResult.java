package com.dzy.gulimall.search.vo;

import com.dzy.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SearchResult {

    private List<SkuEsModel> products;  //查询到的商品信息

    //分页信息
    private Integer pageNum;    //当前页码
    private Integer total;      //总记录数
    private Integer totalPages;     //总页码数
    private List<Integer> pageNavs;     //分页的页码导航

    private List<BrandVo> brands;   //查询到的品牌信息
    private List<CatalogVo> catalogs;    //查询到的分类信息
    private List<AttrVo> attrs;         //查询到的属性信息

    private List<NavVo> navs;       //面包屑导航栏

    private List<Long> attrIds = new ArrayList<>();      //查询参数中包含的属性id，用于页面剔除属性筛选条件

    @Data
    public static class NavVo {
        private String attrName;
        private String attrValue;
        private String link;
    }


    @Data
    public static class BrandVo {
        private Long brandId;
        private String brandName;
        private String brandImg;
    }

    @Data
    public static class CatalogVo {
        private Long catalogId;
        private String catalogName;
    }

    @Data
    public static class AttrVo {
        private Long attrId;
        private String attrName;
        private List<String> attrValues;
    }
}
