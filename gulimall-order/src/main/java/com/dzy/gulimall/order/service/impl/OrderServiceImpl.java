package com.dzy.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.dzy.common.utils.R;
import com.dzy.common.vo.UserRespVo;
import com.dzy.gulimall.order.feign.CartFeignService;
import com.dzy.gulimall.order.feign.MemberFeignService;
import com.dzy.gulimall.order.feign.ProductFeignService;
import com.dzy.gulimall.order.feign.WareFeignService;
import com.dzy.gulimall.order.interceptor.LoginUserInterceptor;
import com.dzy.gulimall.order.vo.MemberAddressVo;
import com.dzy.gulimall.order.vo.OrderConfirmVo;
import com.dzy.gulimall.order.vo.OrderItemHasStockVo;
import com.dzy.gulimall.order.vo.OrderItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzy.common.utils.PageUtils;
import com.dzy.common.utils.Query;

import com.dzy.gulimall.order.dao.OrderDao;
import com.dzy.gulimall.order.entity.OrderEntity;
import com.dzy.gulimall.order.service.OrderService;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import rx.Completable;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 获取订单确认页的封装对象
     */
    @Override
    public OrderConfirmVo confirmOrder() {
        UserRespVo user = LoginUserInterceptor.userThreadLocal.get();
        OrderConfirmVo orderConfirm = new OrderConfirmVo();
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        //1.远程调用查询用户的收货地址
        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<MemberAddressVo> addresses = memberFeignService.getAddressesByMemberId(user.getId());
            orderConfirm.setAddresses(addresses);
        }, executor);
        //2.远程调用查询购物车中选中的购物项
        CompletableFuture<Void> orderItemsFuture = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> orderItems = cartFeignService.getCurrentCartItems();
            orderItems = orderItems.stream().filter(OrderItemVo::getCheck).peek(item -> {
                //查询最新的价格设置进去
                BigDecimal newlyPrice = productFeignService.getNewlyPriceBySkuId(item.getSkuId());
                item.setPrice(newlyPrice);
            }).collect(Collectors.toList());
            orderConfirm.setOrderItems(orderItems);
        }, executor).thenRunAsync(() -> {
            List<OrderItemVo> orderItems = orderConfirm.getOrderItems();
            List<Long> skuIds = orderItems.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
            R r = wareFeignService.getHasStockBySkuIds(skuIds);
            if(r.getCode() == 0) {
                List<OrderItemHasStockVo> hasStocks = r.getData(new TypeReference<List<OrderItemHasStockVo>>() {});
                Map<Long, Boolean> hasStockMap = hasStocks.stream().
                        collect(Collectors.toMap(OrderItemHasStockVo::getSkuId, OrderItemHasStockVo::getHasStock));
                orderConfirm.setHasStockMap(hasStockMap);
            }
        }, executor);
        try {
            CompletableFuture.allOf(getAddressFuture, orderItemsFuture).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //3.用户的积分
        orderConfirm.setIntegration(user.getIntegration());
        //4.计算其他的属性
        //TODO 5.防止重复下单
        return orderConfirm;
    }

}