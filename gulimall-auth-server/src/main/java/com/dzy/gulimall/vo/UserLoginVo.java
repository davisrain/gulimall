package com.dzy.gulimall.vo;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;

@Data
public class UserLoginVo {
    @NotEmpty(message = "用户名不能为空")
    private String account;
    @NotEmpty(message = "密码不能为空")
    private String password;
}
