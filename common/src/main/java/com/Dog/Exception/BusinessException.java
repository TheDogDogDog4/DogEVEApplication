package com.Dog.Exception;

import com.Dog.Doman.ResultEnum;
import lombok.Data;

@Data
public class BusinessException extends RuntimeException{
    private int code;
    private String msg;

    public BusinessException(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public BusinessException(ResultEnum resultEnum) {
        super(resultEnum.getMsg());
        this.code = resultEnum.getCode();
        this.msg = resultEnum.getMsg();
    }
}
