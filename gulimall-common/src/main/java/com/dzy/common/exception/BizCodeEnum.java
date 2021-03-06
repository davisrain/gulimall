package com.dzy.common.exception;

/**
 * 错误码列表：
 * 10：通用
 *      001：参数格式校验
 *      002：短信验证码频率太高
 * 11：商品
 * 12：订单
 * 13：购物车
 * 14：物流
 * 15：用户
 * 21: 库存
 */


public enum BizCodeEnum {
    UNKNOWN_EXCEPTION(10000, "系统未知异常"),
    VALID_EXCEPTION(10001, "数据校验出现问题"),
    SMS_SENDCODE_EXCEPTION(10002, "验证码发送太过频繁，请稍后再试"),
    TOO_MANY_REQUESTS_EXCEPTION(10003, "请求数量过多"),
    PRODUCT_UP_EXCEPTION(11000, "商品上架异常"),
    USER_EXIST_EXCEPTION(15001, "用户已经存在"),
    PHONE_EXIST_EXCEPTION(15002, "手机号已经存在"),
    ACCOUNT_PASSWORD_INVALID_EXCEPTION(15003, "账号或密码错误"),
    WARE_UNKNOWN_EXCEPTION(21000, "库存服务未知异常"),
    NO_STOCK_EXCEPTION(21001, "商品库存不足");
    private int code;
    private String msg;
    BizCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
    public int getCode() {
        return code;
    }
    public String getMsg() {
        return msg;
    }
}
