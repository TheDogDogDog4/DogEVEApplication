package com.dog.usersystem.service;

import com.Dog.Doman.Result;
import com.dog.usersystem.doman.vo.req.UserLoginReq;
import com.dog.usersystem.doman.vo.req.UserRegisterReq;
import com.dog.usersystem.doman.vo.resp.JwtTokenResp;
import org.springframework.http.ResponseEntity;

// 用户操作业务类
public interface UserService {
    Result<Void> registerUser(UserRegisterReq userRegisterReq);
    Result<JwtTokenResp> loginUser(UserLoginReq userLoginReq);
    Result<Void> logoutUser(Long userId);
    Result<JwtTokenResp> refreshToken(Long userId);
}
