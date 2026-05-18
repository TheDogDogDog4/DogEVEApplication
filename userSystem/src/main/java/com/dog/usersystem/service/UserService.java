package com.dog.usersystem.service;

import com.Dog.Doman.Result;
import com.Dog.Doman.dto.req.UserLoginReq;
import com.Dog.Doman.dto.req.UserRegisterReq;
import com.Dog.Doman.dto.resp.JwtTokenResp;

// 用户操作业务类
public interface UserService {
    Result<Void> registerUser(UserRegisterReq userRegisterReq);
    Result<JwtTokenResp> loginUser(UserLoginReq userLoginReq);
    Result<Void> logoutUser(Long userId);
    Result<JwtTokenResp> refreshToken(Long userId);
}
