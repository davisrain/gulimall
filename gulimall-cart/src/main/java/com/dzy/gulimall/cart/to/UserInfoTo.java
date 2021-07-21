package com.dzy.gulimall.cart.to;

import lombok.Data;

@Data
public class UserInfoTo {
    private Long userId;
    private String userKey;
    private Boolean hasTempUser = false;
}
