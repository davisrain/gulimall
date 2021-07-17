package com.dzy.gulimall.member.exception;

public class PhoneExistException extends Exception {
    public PhoneExistException() {
        super("手机号码已经存在");
    }
}
