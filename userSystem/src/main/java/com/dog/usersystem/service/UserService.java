package com.dog.usersystem.service;

import com.dog.usersystem.doman.Result;
import com.dog.usersystem.doman.vo.req.UserLoginReq;
import com.dog.usersystem.doman.vo.req.UserRegisterReq;
import com.dog.usersystem.doman.vo.resp.JwtTokenResp;
import org.springframework.http.ResponseEntity;

// 用户操作业务类
public interface UserService {
    ResponseEntity<Result<Void>> registerUser(UserRegisterReq userRegisterReq);
    ResponseEntity<Result<JwtTokenResp>> loginUser(UserLoginReq userLoginReq);
    ResponseEntity<Result<Void>> logoutUser(Long userId);
    ResponseEntity<Result<JwtTokenResp>> refreshToken(Long userId, String refreshToken);
}
