package com.dzy.common.constant;

public class OrderConstant {
    public static final String ORDER_TOKEN_PREFIX = "order:token:";

    public enum Status {
        //订单状态【0->待付款；1->待发货；2->已发货；3->已完成；4->已关闭；5->无效订单】
        WAIT_PAY(0, "待付款"), WAIT_SEND(1, "待发货"),
        SEND(2, "已发货"), COMPLETED(3, "已完成"),
        CLOSED(4, "已关闭"), INVALID(5, "无效订单");
        private int code;
        private String msg;
        Status(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }
    }

    public enum DeleteStatus {
        //删除状态【0->未删除；1->已删除】
        NOT_DELETE(0, "未删除"), DELETED(1, "已删除");
        private int code;
        private String msg;
        DeleteStatus(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }
    }
}
