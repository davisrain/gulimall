package com.dzy.gulimall.ware.to;

import lombok.Data;

import java.util.List;

@Data
public class WareLockTo {
    private String orderSn;
    private List<LockOrderItemTo> lockOrderItems;
}
