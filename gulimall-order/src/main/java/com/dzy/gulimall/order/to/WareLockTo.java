package com.dzy.gulimall.order.to;

import com.dzy.gulimall.order.vo.OrderItemVo;
import lombok.Data;

import java.util.List;

@Data
public class WareLockTo {
    private String orderSn;
    private List<LockOrderItemTo> lockOrderItems;
}
