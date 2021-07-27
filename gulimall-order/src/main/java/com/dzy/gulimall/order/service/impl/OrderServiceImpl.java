package com.dzy.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.dzy.common.constant.OrderConstant;
import com.dzy.common.utils.R;
import com.dzy.common.vo.UserRespVo;
import com.dzy.gulimall.order.entity.OrderItemEntity;
import com.dzy.gulimall.order.feign.CartFeignService;
import com.dzy.gulimall.order.feign.MemberFeignService;
import com.dzy.gulimall.order.feign.ProductFeignService;
import com.dzy.gulimall.order.feign.WareFeignService;
import com.dzy.gulimall.order.interceptor.LoginUserInterceptor;
import com.dzy.gulimall.order.to.OrderCreateTo;
import com.dzy.gulimall.order.vo.FareVo;
import com.dzy.gulimall.order.vo.MemberAddressVo;
import com.dzy.gulimall.order.vo.OrderConfirmVo;
import com.dzy.gulimall.order.vo.OrderItemHasStockVo;
import com.dzy.gulimall.order.vo.OrderItemVo;
import com.dzy.gulimall.order.vo.OrderSubmitResponseVo;
import com.dzy.gulimall.order.vo.OrderSubmitVo;
import com.dzy.gulimall.order.vo.SpuInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzy.common.utils.PageUtils;
import com.dzy.common.utils.Query;

import com.dzy.gulimall.order.dao.OrderDao;
import com.dzy.gulimall.order.entity.OrderEntity;
import com.dzy.gulimall.order.service.OrderService;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import rx.Completable;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    private ThreadLocal<OrderSubmitVo> orderSubmitThreadLocal = new ThreadLocal<OrderSubmitVo>();

    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

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
        //TODO 5.防止重复下单,使用token进行接口幂等性验证
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = OrderConstant.ORDER_TOKEN_PREFIX + user.getId();
        //将token放入redis
        redisTemplate.opsForValue().set(key, token, 30L, TimeUnit.SECONDS);
        //将token传给前端
        orderConfirm.setOrderToken(token);
        return orderConfirm;
    }

    @Override
    public OrderSubmitResponseVo submitOrder(OrderSubmitVo orderSubmitVo) {
        OrderSubmitResponseVo orderSubmitResponse = new OrderSubmitResponseVo();
        UserRespVo user = LoginUserInterceptor.userThreadLocal.get();
        //下单：验令牌，创建订单，验价格，锁库存...
        //1.原子验令牌
        String orderToken = orderSubmitVo.getOrderToken();
        String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long response = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
                Collections.singletonList(OrderConstant.ORDER_TOKEN_PREFIX + user.getId()), orderToken);
        //返回1表示删除成功即验证成功，0表示验证失败
        if(response == 0) {
            //令牌验证失败
            orderSubmitResponse.setCode(1);
        } else {
            //令牌验证成功，下单
            OrderCreateTo orderCreate = createOrder();

        }
        return orderSubmitResponse;
    }

    /**
     *  生成订单创建对象的方法
     */
    private OrderCreateTo createOrder() {
        OrderCreateTo orderCreate = new OrderCreateTo();
        //1.生成订单号
        String orderSn = IdWorker.getTimeId();
        //2.创建订单
        OrderEntity order = buildOrder(orderSn);
        orderCreate.setOrder(order);
        //3.创建订单项
        List<OrderItemEntity> orderItems = buildOrderItems(orderSn);
        orderCreate.setOrderItems(orderItems);
        //4.验价
        computePrice(order, orderItems);
        return orderCreate;
    }

    /**
     * 验价
     */
    private void computePrice(OrderEntity order, List<OrderItemEntity> orderItems) {

    }

    /**
     *  构建订单数据
     */
    private OrderEntity buildOrder(String orderSn) {
        UserRespVo user = LoginUserInterceptor.userThreadLocal.get();
        OrderEntity order = new OrderEntity();
        order.setOrderSn(orderSn);
        order.setMemberId(user.getId());
        order.setMemberUsername(user.getUsername());
        order.setCreateTime(new Date());
        OrderSubmitVo orderSubmit = orderSubmitThreadLocal.get();
        Long addressId = orderSubmit.getAddressId();
        //远程调用查询物流信息
        R r = wareFeignService.getFare(addressId);
        FareVo fareVo = r.getData(new TypeReference<FareVo>() {});
        //设置运费信息
        order.setFreightAmount(fareVo.getFare());
        //设置收货人信息
        MemberAddressVo address = fareVo.getAddress();
        order.setReceiverProvince(address.getProvince());
        order.setReceiverCity(address.getCity());
        order.setReceiverRegion(address.getRegion());
        order.setReceiverDetailAddress(address.getDetailAddress());
        order.setReceiverPostCode(address.getPostCode());
        order.setReceiverName(address.getName());
        order.setReceiverPhone(address.getPhone());
        return order;
    }

    /**
     *  构建订单项数据
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        List<OrderItemVo> orderItemVos = cartFeignService.getCurrentCartItems();
        List<OrderItemEntity> orderItems = orderItemVos.stream().map(orderItemVo -> {
            OrderItemEntity orderItem = buildOrderItem(orderItemVo);
            orderItem.setOrderSn(orderSn);
            return orderItem;
        }).collect(Collectors.toList());
        return orderItems;
    }

    /**
     *  构建单个订单项数据
     */
    private OrderItemEntity buildOrderItem(OrderItemVo orderItemVo) {
        OrderItemEntity orderItem = new OrderItemEntity();
        //1.订单信息
        //2.spu信息
        Long skuId = orderItemVo.getSkuId();
        R r = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo spuInfo = r.getData(new TypeReference<SpuInfoVo>() {});
        orderItem.setSpuId(spuInfo.getId());
        orderItem.setSpuBrand(spuInfo.getBrandId().toString());
        orderItem.setSpuName(spuInfo.getSpuName());
//        orderItem.setSpuPic();
        //3.sku信息
        orderItem.setSkuId(orderItemVo.getSkuId());
        orderItem.setSkuName(orderItemVo.getTitle());
        orderItem.setSkuPic(orderItemVo.getImage());
        orderItem.setSkuPrice(orderItemVo.getPrice());
        orderItem.setSkuQuantity(orderItemVo.getCount());
        List<String> saleAttrValues = orderItemVo.getSaleAttrs();
        String skuAttrVals = StringUtils.collectionToDelimitedString(saleAttrValues, ";");
        orderItem.setSkuAttrsVals(skuAttrVals);
        //4.优惠信息（不做）
        //5.积分信息
        orderItem.setGiftGrowth(orderItemVo.getPrice().intValue());
        orderItem.setGiftIntegration(orderItemVo.getPrice().intValue());
        //6.价格信息
        orderItem.setPromotionAmount(new BigDecimal("0"));
        orderItem.setCouponAmount(new BigDecimal("0"));
        orderItem.setIntegrationAmount(new BigDecimal("0"));
        BigDecimal totalPrice = orderItem.getSkuPrice().multiply(new BigDecimal(orderItem.getSkuQuantity().toString()));
        BigDecimal realAmount = totalPrice.subtract(orderItem.getPromotionAmount())
                .subtract(orderItem.getCouponAmount()).subtract(orderItem.getIntegrationAmount());
        orderItem.setRealAmount(realAmount);
        return orderItem;
    }

}