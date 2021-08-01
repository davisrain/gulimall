package com.dzy.gulimall.ware.dao;

import com.dzy.gulimall.ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品库存
 * 
 * @author zhengyu_dai
 * @email zhengyu_dai@foxmail.com
 * @date 2021-05-24 23:29:19
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

    void addStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("skuNum") Integer skuNum);

    Integer getStock(Long skuId);

    List<Long> listHasStockWareIdsBySkuId(Long skuId);

    Long lockStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("lockNum") Integer lockNum);

    void unlockStock(@Param("skuId") Long skuId, @Param("unlockNum") Integer unlockNum, @Param("wareId") Long wareId);
}
