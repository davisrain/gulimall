package com.dzy.gulimall.member.exception;

public class UsernameExistException extends Exception{

    public UsernameExistException() {
        super("用户名已经存在");
    }
}
