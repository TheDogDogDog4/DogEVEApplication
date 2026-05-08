package com.dog.usersystem.controller;

import com.dog.usersystem.doman.Result;
import com.dog.usersystem.doman.vo.req.UserLoginReq;
import com.dog.usersystem.doman.vo.req.UserRegisterReq;
import com.dog.usersystem.doman.vo.resp.JwtTokenResp;
import com.dog.usersystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 用户操作控制类
@RestController
@RequestMapping("/auth")
public class UserController {

    // 自动注入 Service 层
    @Autowired
    private UserService userService;

    // 注册接口
    @PostMapping("/register")
    public ResponseEntity<Result<Void>> registerUser(@RequestBody UserRegisterReq userRegisterReq) {
        return userService.registerUser(userRegisterReq);
    }

    // 登录接口
    @PostMapping("/login")
    public ResponseEntity<Result<JwtTokenResp>> loginUser(@RequestBody UserLoginReq userLoginReq) {
        return userService.loginUser(userLoginReq);
    }

    // 登出接口
    @PostMapping("logout")
    public ResponseEntity<Result<Void>> logoutUser(@RequestHeader("userId") Long userId) {
        return userService.logoutUser(userId);
    }

    // Token 刷新
    @PutMapping("/refresh")
    public ResponseEntity<Result<JwtTokenResp>> refreshToken(@RequestHeader("userId") Long userId, @RequestHeader("Authorization") String token) {
        return  userService.refreshToken(userId, token);
    }


}
