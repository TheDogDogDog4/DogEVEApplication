package com.dog.usersystem.service.Impl;

import com.Dog.Doman.Result;
import com.Dog.Doman.ResultEnum;
import com.Dog.Exception.BusinessException;
import com.Dog.Utils.JwtUtil;
import com.Dog.Utils.RedisUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dog.usersystem.dao.UserMapper;
import com.dog.usersystem.doman.dto.User;
import com.dog.usersystem.doman.vo.req.UserLoginReq;
import com.dog.usersystem.doman.vo.req.UserRegisterReq;
import com.dog.usersystem.doman.vo.resp.JwtTokenResp;
import com.dog.usersystem.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
    public Result<Void> registerUser(UserRegisterReq userRegisterReq) {
        // 检查用户名是否存在
        if (userMapper.selectOne(new QueryWrapper<User>().eq("username", userRegisterReq.getUsername())) != null) {
            throw new BusinessException(ResultEnum.USERNAME_EXISTS);
        }

        // 密码加密
        String newPassword = passwordEncoder.encode(userRegisterReq.getPassword());

        // 存入数据库
        User user = new User();
        BeanUtils.copyProperties(userRegisterReq, user);
        user.setPassword(newPassword);
        userMapper.insert(user);

        return Result.success();
    }

    @Override
    public Result<JwtTokenResp> loginUser(UserLoginReq userLoginReq) {
        // 查询用户名
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", userLoginReq.getUsername()));

        // 检验用户有效性
        if (user == null) {
            throw new BusinessException(ResultEnum.USERNAME_NOT_EXITS);
        }

        // 检验密码(非明文)
        if (!passwordEncoder.matches(userLoginReq.getPassword(), user.getPassword())) {
            redisUtil.setUsernameBlackList(userLoginReq.getUsername(), USERNAME_EXPIRE);
            throw new BusinessException(ResultEnum.PASSWORD_INCORRECT);
        }

        // 生成 JWT 令牌
        JwtTokenResp jwtTokenResp = new JwtTokenResp();
        jwtTokenResp.setAccessToken(JwtUtil.createAccessToken(user.getUserId()));
        jwtTokenResp.setRefreshToken(JwtUtil.createRefreshToken(user.getUserId()));

        // redis 缓存 token
        redisUtil.set("user_access_" + user.getUserId(), jwtTokenResp.getAccessToken(), JwtUtil.ACCESS_EXPIRE / 1000);
        redisUtil.set("user_refresh_" + user.getUserId(),  jwtTokenResp.getRefreshToken(), JwtUtil.REFRESH_EXPIRE / 1000);

        redisUtil.deleteUsernameBlackList(userLoginReq.getUsername());
        return Result.success(jwtTokenResp);
    }

    @Override
    public Result<Void> logoutUser(Long userId) {
        // 查token
        String accessToken = (String) redisUtil.get("user_access_" + userId);
        String refreshToken = (String) redisUtil.get("user_refresh_" + userId);

        // token 加入黑名单
        if (accessToken != null) {
            redisUtil.setBlackList(accessToken, JwtUtil.getRemainingExpireSeconds(accessToken));
            redisUtil.delete("user_access_" + userId);
        }
        if (refreshToken != null) {
            redisUtil.setBlackList(refreshToken, JwtUtil.getRemainingExpireSeconds(refreshToken));
            redisUtil.delete("user_refresh_" + userId);
        }

        return Result.success();
    }

    @Override
    public Result<JwtTokenResp> refreshToken(Long userId) {
        String accessToken = JwtUtil.createAccessToken(userId);
        String refreshToken = JwtUtil.createRefreshToken(userId);

        // 刷新 redis
        redisUtil.set("user_access_" + userId, accessToken, JwtUtil.ACCESS_EXPIRE / 1000);
        redisUtil.set("user_refresh_" + userId, refreshToken, JwtUtil.REFRESH_EXPIRE / 1000);

        JwtTokenResp jwtTokenResp = new JwtTokenResp();
        jwtTokenResp.setAccessToken(accessToken);
        jwtTokenResp.setRefreshToken(refreshToken);

        return Result.success(jwtTokenResp);
    }
}
