package com.dog.usersystem.doman.vo.req;

import lombok.Data;

// 用户登录请求类
@Data
public class UserLoginReq {
    private String username;
    private String password;
}
