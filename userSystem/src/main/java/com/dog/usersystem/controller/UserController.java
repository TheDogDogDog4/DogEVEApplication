package com.dog.usersystem.controller;

import com.Dog.Doman.Result;
import com.dog.usersystem.doman.vo.req.UserLoginReq;
import com.dog.usersystem.doman.vo.req.UserRegisterReq;
import com.dog.usersystem.doman.vo.resp.JwtTokenResp;
import com.dog.usersystem.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
    public Result<Void> registerUser(@Valid @RequestBody UserRegisterReq userRegisterReq) {
        return userService.registerUser(userRegisterReq);
    }

    // 登录接口
    @PostMapping("/login")
    public Result<JwtTokenResp> loginUser(@Valid @RequestBody UserLoginReq userLoginReq) {
        return userService.loginUser(userLoginReq);
    }

    // 登出接口
    @PostMapping("logout")
    public Result<Void> logoutUser(@RequestHeader("userId") Long userId) {
        return userService.logoutUser(userId);
    }

    // Token 刷新
    @PutMapping("/refresh")
    public Result<JwtTokenResp> refreshToken(@RequestHeader("userId") Long userId) {
        return  userService.refreshToken(userId);
    }


}
