package com.dzy.gulimall.to;

import lombok.Data;

@Data
public class WeiboAccessTokenTo {
    private String accessToken;
    private String expiresIn;
    private String remindIn;
    private String uid;
}
