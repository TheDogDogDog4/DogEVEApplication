package com.dog.usersystem.utils;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.Random;
import java.util.concurrent.TimeUnit;

// Rides 工具类
@Component
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;
    private final Random RANDOM = new Random();

    public RedisUtil(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 存 (redis)
    public void set(String key, Object value, long expireSeconds) {
        // 防雪崩
        long finalExpireTime = expireSeconds + RANDOM.nextInt(300);

        redisTemplate.opsForValue().set(key, value, finalExpireTime);
    }

    // 查 (redis)
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // 删 (redis)
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    // 存 Null
    public void setNull(String key) {
        redisTemplate.opsForValue().set(key, null);
    }

    // 获取锁
    public boolean lock(String key, long expireSeconds) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "lock",expireSeconds, TimeUnit.SECONDS));
    }

    // 删除锁
    public void unlock(String key) {
        redisTemplate.delete(key);
    }

    // token 加黑名单
    public void setBlackList(String token, long expireSeconds) {
        redisTemplate.opsForValue().set("token_black_list" + token, "1", expireSeconds, TimeUnit.SECONDS );
    }

    // 在黑名单否
    public Boolean isBlackList(String token) {
        return redisTemplate.opsForValue().get("token_black_list" + token) != null;
    }
}
