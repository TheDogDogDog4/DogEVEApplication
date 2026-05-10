package com.Dog.Handler;

import com.Dog.Doman.Result;
import com.Dog.Doman.ResultEnum;
import com.Dog.Exception.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.HashMap;
import java.util.Map;

// 全局异常处理器
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 捕获 @Valid 校验失败的异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Map<String, String>>> handleValid(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();

        e.getBindingResult().getAllErrors().forEach(err -> {
            String field = ((FieldError) err).getField();
            errors.put(field, err.getDefaultMessage());
        });

        return ResponseEntity.status(ResultEnum.PARAM_ERROR.getCode()).body(Result.error(ResultEnum.PARAM_ERROR.getCode(), ResultEnum.PARAM_ERROR.getMsg(), errors));
    }

    // 捕获业务逻辑异常
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        return ResponseEntity.status(ResultEnum.BUSINESS_ERROR.getCode()).body(Result.error(ResultEnum.BUSINESS_ERROR.getCode(), ResultEnum.BUSINESS_ERROR.getMsg()));
    }

    // 捕获全局异常
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(ResultEnum.SYSTEM_ERROR.getCode()).body(Result.error(ResultEnum.SYSTEM_ERROR.getCode(), ResultEnum.SYSTEM_ERROR.getMsg()));
    }
}
