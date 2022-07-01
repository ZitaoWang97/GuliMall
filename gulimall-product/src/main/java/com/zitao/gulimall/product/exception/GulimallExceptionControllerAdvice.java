package com.zitao.gulimall.product.exception;

import com.zitao.common.exception.BizCodeEnume;
import com.zitao.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 集中处理所有异常
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.zitao.gulimall.product.controller")
public class GulimallExceptionControllerAdvice {

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public R handleValidException(MethodArgumentNotValidException e) {
        log.error("数据校验出现问题:{}, 异常类型:{}", e.getMessage(), e.getClass());
        // 拿到所有的异常消息
        BindingResult bindingResult = e.getBindingResult();
        Map<String, String> map = new HashMap<>();
        bindingResult.getFieldErrors().forEach((item) -> {
            String defaultMessage = item.getDefaultMessage();
            String field = item.getField();
            map.put(field, defaultMessage);
        });
        return R.error(BizCodeEnume.VALID_EXCEPTION.getCode(), BizCodeEnume.VALID_EXCEPTION.getMsg()).put("data", map);
    }

    @ExceptionHandler(value = Throwable.class)
    public R handleException(Throwable throwable) {
        return R.error(BizCodeEnume.UNKNOWN_EXCEPTION.getCode(), BizCodeEnume.UNKNOWN_EXCEPTION.getMsg());
    }
}
