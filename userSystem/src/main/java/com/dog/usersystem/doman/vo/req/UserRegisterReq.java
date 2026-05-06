package com.dog.usersystem.doman.vo.req;

import lombok.Data;

// 用户注册请求类
@Data
public class UserRegisterReq {
    private String username;
    private String password;
    private String email;
}
