package com.dzy.gulimall.product.exception;

import com.dzy.common.exception.BizCodeEnum;
import com.dzy.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice(basePackages = "com.dzy.gulimall.product.controller")
public class GulimallExceptionControllerAdvice {

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public R validExceptionHandler(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        Map<String, String> params = new HashMap<>();
        bindingResult.getFieldErrors().forEach((item) -> {
            String fieldName = item.getField();
            String message = item.getDefaultMessage();
            params.put(fieldName, message);
        });
        return R.error(BizCodeEnum.VALID_EXCEPTION).put("data", params);
    }

    @ExceptionHandler(value = Throwable.class)
    public R commonExceptionHandler(Throwable t) {
        log.error("异常信息为：{}", t.getMessage());
        return R.error(BizCodeEnum.UNKNOWN_EXCEPTION);
    }
}
