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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;


// 用户操作业务实现类
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RedisUtil redisUtil;

    @Override
    public ResponseEntity<Result<Void>> registerUser(UserRegisterReq userRegisterReq) {
        try {
            // 1. 检查用户名是否存在
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("username", userRegisterReq.getUsername());
            if (userMapper.selectOne(queryWrapper) != null) {
                return ResponseEntity.status(500).body(Result.error("用户名已存在"));
            }

            // 2. 对象拷贝
            User user = new User();
            BeanUtils.copyProperties(userRegisterReq, user);

            // 3. 密码加密（这里最容易报错）
            String password = userRegisterReq.getPassword();
            String enPassword = passwordEncoder.encode(password);
            user.setPassword(enPassword);

            // 4. 时间
            user.setCreateTime(LocalDateTime.now());

            // 5. 入库
            userMapper.insert(user);

            // 6. 成功
            return ResponseEntity.ok(Result.success());

        } catch (Exception e) {
            // 🔥 捕获所有异常，不让它 500
            e.printStackTrace();
            return ResponseEntity.status(500).body(Result.error("注册失败：" + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<Result<JwtTokenResp>> loginUser(UserLoginReq userLoginReq) {

        // 条件查询
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", userLoginReq.getUsername());
        User user = userMapper.selectOne(queryWrapper);

        // 检验用户有效性
        if (user == null) {
            return ResponseEntity.status(500).body(Result.error());
        }

        // 检验密码（加密后检验， 保证用户密码的安全）
        if (!passwordEncoder.matches(userLoginReq.getPassword(), user.getPassword())) {
            return ResponseEntity.status(500).body(Result.error());
        }

        // 生成 JWT 令牌
        JwtTokenResp jwtTokenResp = new JwtTokenResp();
        jwtTokenResp.setAccessToken(JwtUtil.createAccessToken(user.getUserId()));
        jwtTokenResp.setRefreshToken(JwtUtil.createRefreshToken(user.getUserId()));

        // redis 缓存 token
        redisUtil.set("user_access_" + user.getUserId(), jwtTokenResp.getAccessToken(), JwtUtil.ACCESS_EXPIRE / 1000);
        redisUtil.set("user_refresh_" + user.getUserId(),  jwtTokenResp.getRefreshToken(), JwtUtil.REFRESH_EXPIRE / 1000);

        // 响应结果
        return ResponseEntity.ok(Result.success(jwtTokenResp));
    }

    @Override
    public ResponseEntity<Result<Void>> logoutUser(String token, String refreshToken) {

        // 获取 accessToken
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // token 加入黑名单
        redisUtil.setBlackList(token, JwtUtil.getRemainingExpireSeconds(token));
        redisUtil.setBlackList(refreshToken, JwtUtil.getRemainingExpireSeconds(refreshToken));

        // 响应结果
        return ResponseEntity.ok(Result.success());
    }

    @Override
    public ResponseEntity<Result<JwtTokenResp>> refreshToken(String token) {

        // 获取 token
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 检验是否为 refreshToken
        if (!JwtUtil.isRefreshToken(token)) {
            return ResponseEntity.status(500).body(Result.error());
        }

        Long userId = JwtUtil.getUserId(token);

        String redisToken = (String) redisUtil.get("user_refresh_" + userId);

        // 检查 redis 是否有这个用户的 token
        if (redisToken == null  || !redisToken.equals(token)) {
            return ResponseEntity.status(500).body(Result.error());
        }

        String accessToken = JwtUtil.createAccessToken(userId);

        // 刷新 redis
        redisUtil.set("user_access_" + userId, accessToken, JwtUtil.ACCESS_EXPIRE / 1000);
        redisUtil.set("user_refresh_" + userId, token, JwtUtil.REFRESH_EXPIRE / 1000);

        JwtTokenResp jwtTokenResp = new JwtTokenResp();
        jwtTokenResp.setAccessToken(accessToken);
        jwtTokenResp.setRefreshToken(token);

        // 响应结果
        return ResponseEntity.ok(Result.success(jwtTokenResp));
    }
}
