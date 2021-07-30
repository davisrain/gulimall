package com.dzy.common.to.mq;

import lombok.Data;

@Data
public class StockLockTo {
    private Long taskId;
    private StockLockDetailTo stockLockDetail;

}
