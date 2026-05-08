package com.dog.usersystem.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dog.usersystem.dao.UserMapper;
import com.dog.usersystem.doman.Result;
import com.dog.usersystem.doman.po.User;
import com.dog.usersystem.doman.vo.req.UserLoginReq;
import com.dog.usersystem.doman.vo.req.UserRegisterReq;
import com.dog.usersystem.doman.vo.resp.JwtTokenResp;
import com.dog.usersystem.service.UserService;
import com.dog.usersystem.utils.JwtUtil;
import com.dog.usersystem.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


// 用户操作业务实现类
@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RedisUtil redisUtil;

    private static final int USERNAME_EXPIRE = 900;

    @Override
    public ResponseEntity<Result<Void>> registerUser(UserRegisterReq userRegisterReq) {
        // 记录日志
        log.info("【用户注册】{}", userRegisterReq);

        // 检查用户名是否存在
        if (userMapper.selectOne(new QueryWrapper<User>().eq("username", userRegisterReq.getUsername())) != null) {
            log.warn("【用户名已存在】");
            return ResponseEntity.status(500).body(Result.error());
        }

        // 密码加密
        String newPassword = passwordEncoder.encode(userRegisterReq.getPassword());
        userRegisterReq.setPassword(newPassword);

        // 存入数据库
        User user = new User();
        BeanUtils.copyProperties(userRegisterReq, user);
        userMapper.insert(user);

        // 记录日志, 响应结果
        log.info("【注册成功】");
        return ResponseEntity.ok(Result.success());
    }

    @Override
    public ResponseEntity<Result<JwtTokenResp>> loginUser(UserLoginReq userLoginReq) {
        // 记录日志
        log.info("【用户登录】{}", userLoginReq);

        // 查询用户名
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", userLoginReq.getUsername()));

        // 检验用户有效性
        if (user == null) {
            log.warn("【用户名不存在】");
            return ResponseEntity.status(500).body(Result.error());
        }

        // 检验密码(非明文)
        if (!passwordEncoder.matches(userLoginReq.getPassword(), user.getPassword())) {
            log.warn("【用户密码错误】");
            redisUtil.setUserBlackList(userLoginReq.getUsername(), USERNAME_EXPIRE);
            return ResponseEntity.status(500).body(Result.error());
        }

        // 生成 JWT 令牌
        JwtTokenResp jwtTokenResp = new JwtTokenResp();
        jwtTokenResp.setAccessToken(JwtUtil.createAccessToken(user.getUserId()));
        jwtTokenResp.setRefreshToken(JwtUtil.createRefreshToken(user.getUserId()));

        // redis 缓存 token
        redisUtil.set("user_access_" + user.getUserId(), jwtTokenResp.getAccessToken(), JwtUtil.ACCESS_EXPIRE / 1000);
        redisUtil.set("user_refresh_" + user.getUserId(),  jwtTokenResp.getRefreshToken(), JwtUtil.REFRESH_EXPIRE / 1000);

        // 记录日志, 响应结果
        log.info("【登陆成功】");
        redisUtil.deleteUsernameBlackList(userLoginReq.getUsername());
        return ResponseEntity.ok(Result.success(jwtTokenResp));
    }

    @Override
    public ResponseEntity<Result<Void>> logoutUser(Long userId) {
        // 记录日志
        log.info("【用户登出】");

        // 查token
        String accessToken = redisUtil.get("user_access_" + userId).toString();
        String refreshToken = redisUtil.get("user_refresh_" + userId).toString();

        // token 加入黑名单
        redisUtil.setBlackList(accessToken, JwtUtil.getRemainingExpireSeconds(accessToken));
        redisUtil.setBlackList(refreshToken, JwtUtil.getRemainingExpireSeconds(refreshToken));

        // 记录日志, 响应结果
        log.info("【登出成功】");
        redisUtil.delete("user_refresh_" + userId);
        return ResponseEntity.ok(Result.success());
    }

    @Override
    public ResponseEntity<Result<JwtTokenResp>> refreshToken(Long userId, String refreshToken) {
        // 记录日志
        log.info("【刷新 Token】");
        String accessToken = JwtUtil.createAccessToken(userId);

        // 刷新 redis
        redisUtil.set("user_access_" + userId, accessToken, JwtUtil.ACCESS_EXPIRE / 1000);
        redisUtil.set("user_refresh_" + userId, refreshToken, JwtUtil.REFRESH_EXPIRE / 1000);

        JwtTokenResp jwtTokenResp = new JwtTokenResp();
        jwtTokenResp.setAccessToken(accessToken);
        jwtTokenResp.setRefreshToken(refreshToken);

        // 记录日志, 响应结果
        log.info("【刷新成功】");
        return ResponseEntity.ok(Result.success(jwtTokenResp));
    }
}
