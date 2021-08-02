package com.dzy.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.dzy.common.constant.OrderConstant;
import com.dzy.common.to.mq.OrderTo;
import com.dzy.common.utils.R;
import com.dzy.common.vo.UserRespVo;
import com.dzy.gulimall.order.entity.OrderItemEntity;
import com.dzy.gulimall.order.feign.CartFeignService;
import com.dzy.gulimall.order.feign.MemberFeignService;
import com.dzy.gulimall.order.feign.ProductFeignService;
import com.dzy.gulimall.order.feign.WareFeignService;
import com.dzy.gulimall.order.interceptor.LoginUserInterceptor;
import com.dzy.gulimall.order.service.OrderItemService;
import com.dzy.gulimall.order.to.LockOrderItemTo;
import com.dzy.gulimall.order.to.OrderCreateTo;
import com.dzy.gulimall.order.to.WareLockTo;
import com.dzy.gulimall.order.vo.FareVo;
import com.dzy.gulimall.order.vo.MemberAddressVo;
import com.dzy.gulimall.order.vo.OrderConfirmVo;
import com.dzy.gulimall.order.vo.OrderItemHasStockVo;
import com.dzy.gulimall.order.vo.OrderItemVo;
import com.dzy.gulimall.order.vo.OrderSubmitResponseVo;
import com.dzy.gulimall.order.vo.OrderSubmitVo;
import com.dzy.gulimall.order.vo.SpuInfoVo;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


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
    OrderItemService orderItemService;

    @Autowired
    RabbitTemplate rabbitTemplate;

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
            if (r.getCode() == 0) {
                List<OrderItemHasStockVo> hasStocks = r.getData(new TypeReference<List<OrderItemHasStockVo>>() {
                });
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

//    @GlobalTransactional 高并发场景下，不能使用seata的分布式事务，因为它是基于锁机制实现的。
    @Transactional      //本地事务，在分布式结构下不能回滚其他服务的事务
    @Override
    public OrderSubmitResponseVo submitOrder(OrderSubmitVo orderSubmitVo) {
        orderSubmitThreadLocal.set(orderSubmitVo);
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
            //1.创建订单，订单项等信息
            OrderCreateTo orderCreate = createOrder();
            //2.验价
            BigDecimal payAmount = orderCreate.getOrder().getPayAmount();
            BigDecimal payPrice = orderSubmitVo.getPayPrice();
            if(Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                //3.验价成功，保存订单
                saveOrder(orderCreate);
                //4.锁定库存
                WareLockTo wareLockTo = new WareLockTo();
                wareLockTo.setOrderSn(orderCreate.getOrder().getOrderSn());
                List<LockOrderItemTo> lockOrderItems = orderCreate.getOrderItems().stream().map(orderItem -> {
                    LockOrderItemTo lockOrderItem = new LockOrderItemTo();
                    lockOrderItem.setSkuId(orderItem.getSkuId());
                    lockOrderItem.setCount(orderItem.getSkuQuantity());
                    return lockOrderItem;
                }).collect(Collectors.toList());
                wareLockTo.setLockOrderItems(lockOrderItems);
                R r = wareFeignService.lockStock(wareLockTo);
                if(r.getCode() == 0) {
                    //锁库存成功
                    orderSubmitResponse.setOrderEntity(orderCreate.getOrder());
                    orderSubmitResponse.setCode(0);
                    //给MQ发送订单创建成功的消息，延时一定时间后检查订单是否支付
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", orderCreate.getOrder());
                } else {
                    //锁库存失败
                    orderSubmitResponse.setCode(3);
                }
            } else {
                //验价失败
                orderSubmitResponse.setCode(2);
            }
        }
        return orderSubmitResponse;
    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
       return getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
    }

    @Override
    public void closeOrder(OrderEntity order) {
        OrderEntity dbOrder = getById(order.getId());
        if(dbOrder != null && dbOrder.getStatus() == OrderConstant.Status.WAIT_PAY.getCode()) {
            //关闭订单
            dbOrder.setStatus(OrderConstant.Status.CLOSED.getCode());
            updateById(dbOrder);
            //关闭订单之后，向库存解锁队列发送一个消息，进行手动库存解锁
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(dbOrder, orderTo);
            rabbitTemplate.convertAndSend("stock-event-exchange", "stock.release", orderTo);
        }
    }

    /**
     * 分页根据用户id获取订单和订单详情
     * @param params
     * @return
     */
    @Override
    public PageUtils pageOrderWithItemsByMemberId(Map<String, Object> params) {
        UserRespVo user = LoginUserInterceptor.userThreadLocal.get();
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id", user.getId()).orderByDesc("id")
        );
        page.getRecords().forEach(order -> {
            List<OrderItemEntity> orderItems = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", order.getOrderSn()));
            order.setOrderItems(orderItems);
        });
        return new PageUtils(page);
    }

    private void saveOrder(OrderCreateTo orderCreate) {
        OrderEntity order = orderCreate.getOrder();
        save(order);
        List<OrderItemEntity> orderItems = orderCreate.getOrderItems();
        orderItemService.saveBatch(orderItems);
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
        //4.计算订单价格等信息
        computePrice(order, orderItems);
        return orderCreate;
    }

    /**
     * 验价
     */
    private void computePrice(OrderEntity order, List<OrderItemEntity> orderItems) {
        //计算价格信息
        BigDecimal totalAmount = new BigDecimal("0");
        BigDecimal promotionAmount = new BigDecimal("0");
        BigDecimal integrationAmount = new BigDecimal("0");
        BigDecimal couponAmount = new BigDecimal("0");
        Integer integration = 0;
        Integer growth = 0;
        for (OrderItemEntity orderItem : orderItems) {
            totalAmount = totalAmount.add(orderItem.getRealAmount());
            promotionAmount = promotionAmount.add(orderItem.getPromotionAmount());
            integrationAmount = integrationAmount.add(orderItem.getIntegrationAmount());
            couponAmount = couponAmount.add(orderItem.getCouponAmount());
            integration += orderItem.getGiftIntegration();
            growth += orderItem.getGiftGrowth();
        }
        order.setTotalAmount(totalAmount);
        order.setPromotionAmount(promotionAmount);
        order.setIntegrationAmount(integrationAmount);
        order.setCouponAmount(couponAmount);
        order.setPayAmount(order.getTotalAmount().add(order.getFreightAmount()));
        //计算积分信息
        order.setIntegration(integration);
        order.setGrowth(growth);
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
        order.setStatus(OrderConstant.Status.WAIT_PAY.getCode());
        order.setDeleteStatus(OrderConstant.DeleteStatus.NOT_DELETE.getCode());
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
        orderItem.setCategoryId(spuInfo.getCatalogId());
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
        orderItem.setGiftGrowth(orderItemVo.getPrice().multiply(new BigDecimal(orderItem.getSkuQuantity().toString())).intValue());
        orderItem.setGiftIntegration(orderItemVo.getPrice().multiply(new BigDecimal(orderItem.getSkuQuantity().toString())).intValue());
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

    /**
     * 事务传播行为演示
     */
    @Transactional(propagation = Propagation.REQUIRED,timeout = 30)
    public void a() {
        b();    //b会加入a的事务，一旦a方法内出现异常，ab方法都会回滚，并且一旦b方法加入a方法的事务，
                // 那它设置的事务属性都会同a方法一样。比如超时时间，b加入a之后，自己事务的2秒超时时间就失效。
        c();    //c会另起一个事务运行，a方法内出现异常，执行过的c不会回滚
        //但是，直接调用本类下的事务方法，事务传播不会起作用。因为@Transactional注解声明事务本质是使用代理来实现的。
        //开启事务等逻辑都在代理对象的b方法中，因此直接调用本类的b方法不会有效果。

        //解决方法：
        //  不能将自己注入自己，可能会导致循环依赖的问题出现
        //  1、引入aop的starter
        //  2、在启动类上标注@EnableAspectJAutoProxy(exposeProxy = true)，开启AspectJ代理（使用CGLib实现动态代理，不需要代理类有实现接口），
        //  默认的JDK动态代理需要被代理的类实现过接口。并且设置注解的属性exposeProxy = true，暴露代理对象。
        //  3、在当前类中可以通过AopContext.currentProxy()方法获取到当前类的代理类。然后调用代理类中的b和c方法。
        OrderServiceImpl orderService = (OrderServiceImpl) AopContext.currentProxy();
        orderService.b();
        orderService.c();
    }

    @Transactional(propagation = Propagation.REQUIRED, timeout = 2)
    public void b() {

    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void c() {

    }

}