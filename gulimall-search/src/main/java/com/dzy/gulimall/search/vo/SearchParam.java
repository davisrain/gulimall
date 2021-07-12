package com.dzy.gulimall.search.vo;


import lombok.Data;

import java.util.List;

@Data
public class SearchParam {

    private String keyword;     //根据关键字搜索skuTitle
    private Long catalog3Id;    //根据三级分类的id
    /*
          排序筛选
          sort = hotScore_asc/desc    热点分
          sort = saleCount_asc/desc   销量
          sort = skuPrice_asc/desc    价格
     */
    private String sort;
    private List<Long> brandId;     //根据品牌筛选，可以多选
    private Integer hasStock = 1;        //根据是否有货进行筛选，hasStock=0/1
    private String skuPrice;        //根据价格区间进行筛选，skuPrice=0_500/_500/500_
    /*
        根据属性筛选
        attr = 1_IOS:安卓         代表属性id为1，选择的值为ios和安卓
        attr = 2_5寸:6寸
     */
    private List<String> attrs;
    private Integer pageNum = 1;        //根据页码查询
}
