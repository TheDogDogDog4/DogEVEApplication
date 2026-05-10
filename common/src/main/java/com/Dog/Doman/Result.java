package com.Dog.Doman;

import lombok.Data;

// 响应通用类
@Data
public class Result<T> {
    private Integer code;
    private String msg;
    private T data;

    // 成功
    public static <T> Result<T> success(){
        Result<T> result=new Result<T>();
        result.setCode(200);
        result.setMsg("success");
        return result;
    }

    public static <T> Result<T> success(T data){
        Result<T> result=new Result<T>();
        result.setCode(200);
        result.setMsg("success");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> success(String msg){
        Result<T> result=new Result<T>();
        result.setCode(200);
        result.setMsg(msg);
        return result;
    }

    public static <T> Result<T> success(String msg, T data){
        Result<T> result=new Result<T>();
        result.setCode(200);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }

    // 失败
    public static <T> Result<T> error(){
        Result<T> result=new Result<T>();
        result.setCode(500);
        result.setMsg("error");
        return result;
    }

    public static <T> Result<T> error(T data){
        Result<T> result=new Result<T>();
        result.setCode(500);
        result.setMsg("error");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(String msg){
        Result<T> result=new Result<T>();
        result.setCode(500);
        result.setMsg(msg);
        return result;
    }

    public static <T> Result<T> error(String msg, T data){
        Result<T> result=new Result<T>();
        result.setCode(500);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(int code, T data){
        Result<T> result=new Result<T>();
        result.setCode(code);
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(int code, String msg){
        Result<T> result=new Result<T>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }

    public static <T> Result<T> error(int code, String msg,  T data){
        Result<T> result=new Result<T>();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }
}
