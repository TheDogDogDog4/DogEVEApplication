package com.dog.usersystem.service.Impl;

import com.Dog.Doman.Result;
import com.Dog.Doman.ResultEnum;
import com.Dog.Doman.dto.postgreSQL.PgUser;
import com.Dog.Exception.BusinessException;
import com.Dog.Utils.JwtUtil;
import com.Dog.Utils.RedisUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dog.usersystem.dao.UserMapper;
import com.Dog.Doman.dto.req.UserLoginReq;
import com.Dog.Doman.dto.req.UserRegisterReq;
import com.Dog.Doman.dto.resp.JwtTokenResp;
import com.dog.usersystem.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
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
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    private static final int USERNAME_EXPIRE = 900;

    private static final long DUPLICATE_EXPIRE = 5;

    private static final long REGISTER_ING = 10;

    @Override
    public Result<Void> registerUser(UserRegisterReq userRegisterReq) {

        String username = userRegisterReq.getUsername();
        String password = userRegisterReq.getPassword();
        String lockKey = "Registering" + username;

        // 记录日志
        log.info("用户注册业务 | {}", userRegisterReq.getUsername());

        // 抢锁
        boolean lock = redisUtil.lock(lockKey, REGISTER_ING);

        if (!lock) {
            log.warn("用户正在注册中，请勿重复提交 | {}", username);
            throw new BusinessException(ResultEnum.USERNAME_EXISTS);
        }

        try {
            // 检查用户名是否存在
            if (userMapper.selectOne(new QueryWrapper<PgUser>().eq("username", username)) != null) {
                log.warn("用户名已存在 | {}", username);
                throw new BusinessException(ResultEnum.USERNAME_EXISTS);
            }

            // 密码加密
            String newPassword = passwordEncoder.encode(password);

            // 存入数据库
            PgUser user = new PgUser();
            BeanUtils.copyProperties(userRegisterReq, user);
            user.setPassword(newPassword);
            userMapper.insert(user);

            // 存数据到es（rocketMQ 消息）
            rocketMQTemplate.convertAndSend("user-register-topic", user.getUserId());

            // 防重复提交
            redisUtil.setDuplicateBlackList("/auth/register", username, DUPLICATE_EXPIRE);
            log.info("登录防重复提交已写入 | {}", username);

            return Result.success();
        } finally {
            redisUtil.delete("Registering" + username);
        }
    }

    @Override
    public Result<JwtTokenResp> loginUser(UserLoginReq userLoginReq) {
        log.info("用户登录业务 | {}", userLoginReq.getUsername());

        // 查询用户名
        PgUser user = userMapper.selectOne(new QueryWrapper<PgUser>().eq("username", userLoginReq.getUsername()));

        // 检验用户有效性
        if (user == null) {
            log.warn("用户不存在 | {}", userLoginReq.getUsername());
            throw new BusinessException(ResultEnum.USERNAME_NOT_EXITS);
        }

        // 检验密码(非明文)
        if (!passwordEncoder.matches(userLoginReq.getPassword(), user.getPassword())) {
            log.warn("密码错误 | {}", userLoginReq.getPassword());
            redisUtil.setUsernameBlackList(userLoginReq.getUsername(), USERNAME_EXPIRE);
            log.info("防暴力破解已写入 | {}", userLoginReq.getUsername());
            throw new BusinessException(ResultEnum.PASSWORD_INCORRECT);
        }

        // 生成 JWT 令牌
        JwtTokenResp jwtTokenResp = new JwtTokenResp();
        jwtTokenResp.setAccessToken(JwtUtil.createAccessToken(user.getUserId()));
        jwtTokenResp.setRefreshToken(JwtUtil.createRefreshToken(user.getUserId()));

        // redis 缓存 token
        redisUtil.set("user_access_" + user.getUserId(), jwtTokenResp.getAccessToken(), JwtUtil.ACCESS_EXPIRE / 1000);
        redisUtil.set("user_refresh_" + user.getUserId(),  jwtTokenResp.getRefreshToken(), JwtUtil.REFRESH_EXPIRE / 1000);

        // 放重复提交
        redisUtil.setDuplicateBlackList("/auth/login", user.getUsername(), DUPLICATE_EXPIRE);
        log.info("登录防重复提交已写入 | {}", user.getUsername());

        redisUtil.deleteUsernameBlackList(userLoginReq.getUsername());
        return Result.success(jwtTokenResp);
    }

    @Override
    public Result<Void> logoutUser(Long userId) {
        log.info("用户登出业务 | {}", userId);

        // 查token
        String accessToken = (String) redisUtil.get("user_access_" + userId);
        String refreshToken = (String) redisUtil.get("user_refresh_" + userId);

        // token 加入黑名单
        if (accessToken != null) {
            log.info("短期 token 加黑名单 | {}", userId);
            redisUtil.setBlackList(accessToken, JwtUtil.getRemainingExpireSeconds(accessToken));
            redisUtil.delete("user_access_" + userId);
        }
        if (refreshToken != null) {
            log.info("长期 token 加黑名单 | {}", userId);
            redisUtil.setBlackList(refreshToken, JwtUtil.getRemainingExpireSeconds(refreshToken));
            redisUtil.delete("user_refresh_" + userId);
        }

        // 放重复提交
        redisUtil.setDuplicateBlackList("/auth/logout", userId, DUPLICATE_EXPIRE);
        log.info("登出防重复提交已写入 | {}", userId);

        return Result.success();
    }

    @Override
    public Result<JwtTokenResp> refreshToken(Long userId) {
        log.info("token 刷新业务 | {}", userId);
        String accessToken = JwtUtil.createAccessToken(userId);
        String refreshToken = JwtUtil.createRefreshToken(userId);

        // 刷新 redis
        redisUtil.set("user_access_" + userId, accessToken, JwtUtil.ACCESS_EXPIRE / 1000);
        redisUtil.set("user_refresh_" + userId, refreshToken, JwtUtil.REFRESH_EXPIRE / 1000);

        JwtTokenResp jwtTokenResp = new JwtTokenResp();
        jwtTokenResp.setAccessToken(accessToken);
        jwtTokenResp.setRefreshToken(refreshToken);

        // 放重复提交
        redisUtil.setDuplicateBlackList("/auth/refresh", userId, DUPLICATE_EXPIRE);
        log.info("token 刷新防重复提交已写入 | {}", userId);

        return Result.success(jwtTokenResp);
    }
}
