package com.dog.evesystem.controller;

import com.dog.evesystem.doman.Result;
import com.dog.evesystem.doman.vo.resp.EVECharacterResp;
import com.dog.evesystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/eve")
public class UserController {
    @Autowired
    private UserService userService;

    // ESI 登录
    @GetMapping("/login")
    public RedirectView loginESI(Long userId) {
        return userService.loginESI(userId);
    }

    // ESI callback 获取 token
    @GetMapping("/callback")
    public ResponseEntity<?> callbackESI(@RequestParam("code") String code, String state) {
        return userService.callbackESI(code, state);
    }

    // 获取角色数据(单个)
    @GetMapping("/info")
    public ResponseEntity<Result<EVECharacterResp>> characterInfo(@RequestHeader("userId") Long userId) {
        return userService.characterInfo(userId);
    }

    // ESI accessToken 刷新
    @PutMapping("/refresh")
    public ResponseEntity<Result<Void>> refreshESIToken(@RequestHeader("userId") Long userId) {
        return userService.refreshESIToken(userId);
    }
}
