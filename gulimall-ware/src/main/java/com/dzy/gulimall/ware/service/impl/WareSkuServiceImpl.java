package com.dzy.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.dzy.common.constant.OrderConstant;
import com.dzy.common.constant.WareConstant;
import com.dzy.common.to.SkuHasStockTo;
import com.dzy.common.to.mq.OrderTo;
import com.dzy.common.to.mq.StockLockDetailTo;
import com.dzy.common.to.mq.StockLockTo;
import com.dzy.common.utils.R;
import com.dzy.gulimall.ware.entity.PurchaseDetailEntity;
import com.dzy.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.dzy.gulimall.ware.entity.WareOrderTaskEntity;
import com.dzy.gulimall.ware.exception.NoStockException;
import com.dzy.gulimall.ware.feign.OrderFeignService;
import com.dzy.gulimall.ware.feign.ProductFeignService;
import com.dzy.gulimall.ware.service.PurchaseDetailService;
import com.dzy.gulimall.ware.service.WareOrderTaskDetailService;
import com.dzy.gulimall.ware.service.WareOrderTaskService;
import com.dzy.gulimall.ware.to.LockOrderItemTo;
import com.dzy.gulimall.ware.to.WareLockTo;
import com.dzy.gulimall.ware.vo.OrderVo;
import com.rabbitmq.client.Channel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dzy.common.utils.PageUtils;
import com.dzy.common.utils.Query;

import com.dzy.gulimall.ware.dao.WareSkuDao;
import com.dzy.gulimall.ware.entity.WareSkuEntity;
import com.dzy.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    PurchaseDetailService purchaseDetailService;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    WareOrderTaskService wareOrderTaskService;

    @Autowired
    WareOrderTaskDetailService wareOrderTaskDetailService;

    @Autowired
    OrderFeignService orderFeignService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    /**
     *  1、订单下单成功，但是由于超时没有支付或者用户自动取消的原因，导致订单状态不成功，需要解锁库存
     *  2、远程调用库存锁定方法成功，但是下单业务在后面的逻辑出现异常，导致订单数据回滚，此时没有订单数据，需要解锁库存
     *  3、库存锁定业务出现异常，导致订单业务也回滚，此时库存和订单、以及库存锁定任务单都没有数据，但是rabbitmq里面可能会有
     *      部分成功锁定库存的消息，此时接收到消息不需要解锁库存。
     */
    @Transactional
    public void releaseStock(StockLockTo stockLockTo) throws Exception {
        StockLockDetailTo stockLockDetail = stockLockTo.getStockLockDetail();
        Long taskDetailId = stockLockDetail.getId();
        WareOrderTaskDetailEntity taskDetail = wareOrderTaskDetailService.getById(taskDetailId);
        if(taskDetail != null) {
            //数据库里有对应的库存锁定任务单详情数据，此时需要根据库存任务单拿到对应的订单号，
            // 查看订单状态(订单可能会没有)来决定是否解锁库存。
            Long taskId = stockLockTo.getTaskId();
            WareOrderTaskEntity task = wareOrderTaskService.getById(taskId);
            String orderSn = task.getOrderSn();
            //远程调用订单服务查询订单
            R r = orderFeignService.getOrderByOrderSn(orderSn);
            if(r.getCode() == 0) {
                OrderVo order = r.getData(new TypeReference<OrderVo>() {});
                //订单不存在或者订单状态为关闭/无效订单都需要解锁库存
                if(order == null
                        || order.getStatus() == OrderConstant.Status.CLOSED.getCode()
                        || order.getStatus() == OrderConstant.Status.INVALID.getCode()) {
                    //需要taskDetail的状态的已锁定时，才需要进行解锁
                    if(taskDetail.getLockStatus() == WareConstant.StockLockStatusEnum.LOCKED.getCode())
                        unlockStock(stockLockDetail.getSkuId(), stockLockDetail.getSkuNum(), stockLockDetail.getWareId(), taskDetailId);
                }
                //手动确认MQ消息，防止消息丢失
//                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            } else{
                //远程调用失败，将消息拒绝并重新入队，让别的消费者可以收到这个消息
//                channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
                throw new RuntimeException("远程调用失败");
            }
        } else {
            //数据库里没有对应的库存锁定任务单详情，说明库存锁定业务出现异常已经回滚，此时不用解锁库存
//            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
    }

    @Override
    @Transactional
    public void releaseStock(OrderTo orderTo) {
        //根据order的订单号拿到任务单的数据
        WareOrderTaskEntity orderTask = wareOrderTaskService.getOne(new QueryWrapper<WareOrderTaskEntity>()
                .eq("order_sn", orderTo.getOrderSn()));
        //根据orderTask的信息拿到任务单详情信息(需要库存状态是已锁定的才需要解锁，防止重复解锁)
        List<WareOrderTaskDetailEntity> orderTaskDetails = wareOrderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>()
                .eq("task_id", orderTask.getId()).eq("lock_status", 1));
        for (WareOrderTaskDetailEntity orderTaskDetail : orderTaskDetails) {
            unlockStock(orderTaskDetail.getSkuId(), orderTaskDetail.getSkuNum(), orderTaskDetail.getWareId(), orderTaskDetail.getId());
        }
    }

    private void unlockStock(Long skuId, Integer unlockNum, Long wareId, Long taskDetailId) {
        //解锁库存
        baseMapper.unlockStock(skuId, unlockNum, wareId);
        //修改库存任务单详情的状态为已解锁
        WareOrderTaskDetailEntity taskDetail = wareOrderTaskDetailService.getById(taskDetailId);
        taskDetail.setLockStatus(WareConstant.StockLockStatusEnum.UNLOCKED.getCode());
        wareOrderTaskDetailService.updateById(taskDetail);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();
        String wareId = (String) params.get("wareId");
        String skuId = (String) params.get("skuId");
        if (StringUtils.isNotBlank(wareId))
            queryWrapper.eq("ware_id", wareId);
        if (StringUtils.isNotBlank(skuId))
            queryWrapper.eq("sku_id", skuId);
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    /**
     * 根据成功的采购单id进行入库
     *
     * @param successPurchaseDetailIds 成功的采购单id
     */
    @Transactional
    @Override
    @SuppressWarnings("unchecked")
    public void addStockByPurchaseDetailIds(List<Long> successPurchaseDetailIds) {
        List<PurchaseDetailEntity> purchaseDetails = purchaseDetailService.listByIds(successPurchaseDetailIds);
        purchaseDetails.forEach(purchaseDetail -> {
            //判断库存中是否有该商品，有就更新，没有就新增
            QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
            wrapper.eq("sku_id", purchaseDetail.getSkuId()).eq("ware_id", purchaseDetail.getWareId());
            WareSkuEntity wareSkuEntity = baseMapper.selectOne(wrapper);
            if (wareSkuEntity == null) {
                WareSkuEntity wareSku = new WareSkuEntity();
                wareSku.setSkuId(purchaseDetail.getSkuId());
                wareSku.setWareId(purchaseDetail.getWareId());
                //使用远程调用获取sku的name,如果远程调用失败，整个事务无需回滚
                //1.使用try-catch包裹，并且忽略catch，达到效果
                //TODO 还可以用什么办法让异常出现后不回滚
                try {
                    R r = productFeignService.info(purchaseDetail.getSkuId());
                    if (r.getCode() == 0) {
                        Map<String, Object> skuInfo = (Map<String, Object>) r.get("skuInfo");
                        wareSku.setSkuName((String) skuInfo.get("skuName"));
                    }
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
                wareSku.setStock(purchaseDetail.getSkuNum());
                wareSku.setStockLocked(0);
                baseMapper.insert(wareSku);
            } else {
                baseMapper.addStock(purchaseDetail.getSkuId(), purchaseDetail.getWareId(), purchaseDetail.getSkuNum());
            }
        });
    }

    @Override
    public List<SkuHasStockTo> getHasStockBySkuIds(List<Long> skuIds) {

        return skuIds.stream().map(skuId -> {
            SkuHasStockTo skuHasStockTo = new SkuHasStockTo();
            Integer stock = baseMapper.getStock(skuId);
            skuHasStockTo.setSkuId(skuId);
            skuHasStockTo.setHasStock(stock != null && stock > 0);
            return skuHasStockTo;
        }).collect(Collectors.toList());
    }


    /**
     * 锁库存
     */
    @Transactional
    @Override
    public void lockStock(WareLockTo wareLockTo) {
        //生成库存任务单以及任务单详情
        WareOrderTaskEntity task = new WareOrderTaskEntity();
        task.setOrderSn(wareLockTo.getOrderSn());
        wareOrderTaskService.save(task);

        List<LockOrderItemTo> lockOrderItems = wareLockTo.getLockOrderItems();
        //1.根据skuId找到有库存的wareIds
        List<HasStockWare> hasStockWares = lockOrderItems.stream().map(lockOrderItem -> {
            HasStockWare hasStockWare = new HasStockWare();
            hasStockWare.setSkuId(lockOrderItem.getSkuId());
            hasStockWare.setLockNum(lockOrderItem.getCount());
            List<Long> hasStockWareIds = baseMapper.listHasStockWareIdsBySkuId(hasStockWare.getSkuId());
            hasStockWare.setHasStockWareIds(hasStockWareIds);
            return hasStockWare;
        }).collect(Collectors.toList());
        for (HasStockWare hasStockWare : hasStockWares) {

            //2.进行锁库存
            Long skuId = hasStockWare.getSkuId();
            List<Long> hasStockWareIds = hasStockWare.getHasStockWareIds();
            if(hasStockWareIds == null || hasStockWareIds.size() == 0 ){
                throw new NoStockException(skuId);
            }
            for (Long hasStockWareId : hasStockWareIds) {
                Long count = baseMapper.lockStock(skuId, hasStockWareId, hasStockWare.getLockNum());
                if(count == 1) {
                    //锁成功了，直接跳出循环，锁下一个sku
                    WareOrderTaskDetailEntity taskDetail = new WareOrderTaskDetailEntity();
                    taskDetail.setSkuId(skuId);
                    taskDetail.setSkuNum(hasStockWare.getLockNum());
                    taskDetail.setTaskId(task.getId());
                    taskDetail.setWareId(hasStockWareId);
                    taskDetail.setLockStatus(WareConstant.StockLockStatusEnum.LOCKED.getCode());
                    //保存任务单详情
                    wareOrderTaskDetailService.save(taskDetail);
                    //通过rabbitTemplate给MQ发消息
                    StockLockTo stockLockTo = new StockLockTo();
                    stockLockTo.setTaskId(task.getId());
                    StockLockDetailTo stockLockDetail = new StockLockDetailTo();
                    BeanUtils.copyProperties(taskDetail, stockLockDetail);
                    stockLockTo.setStockLockDetail(stockLockDetail);
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", stockLockTo);
                    break;
                }
                //否则抛出异常，中断程序
                throw new NoStockException(skuId);
            }
        }


    }

    @Data
    class HasStockWare {
        private Long skuId;
        private Integer lockNum;
        private List<Long> hasStockWareIds;
    }


}