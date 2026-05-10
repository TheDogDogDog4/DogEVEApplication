package com.Dog.Utils;

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

    // 获取锁
    public boolean lock(String key, long expireSeconds) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "lock",expireSeconds, TimeUnit.SECONDS));
    }

    // 删除锁
    public void unlock(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 黑名单操作
     */

    // token 黑名单操作
    public void setBlackList(String token, long expireSeconds) {
        redisTemplate.opsForValue().set("token_black_list" + token, "1", expireSeconds, TimeUnit.SECONDS );
    }

    public Boolean isBlackList(String token) {
        return redisTemplate.opsForValue().get("token_black_list" + token) != null;
    }

    // 暴力破解黑名单
    public void setUsernameBlackList(String username, long expireSeconds) {
        redisTemplate.opsForValue().increment("username_black_list" + username, 1);

        int count = Integer.parseInt(redisTemplate.opsForValue().get("username_black_list" + username).toString());

        if (count == 1) {
            redisTemplate.expire("username_black_list" + username, expireSeconds, TimeUnit.SECONDS);
        }
    }

    public int isUsernameBlackList(String username) {
        Integer count = (Integer) redisTemplate.opsForValue().get("username_black_list" + username);

        return count == null ? 0 : count;
    }

    public void deleteUsernameBlackList(String username) {
        redisTemplate.delete(username);
    }

    // 重复提交黑名单
    public Boolean isDuplicateBlackList(String path, Long userId) {
        return redisTemplate.opsForValue().get("duplicate_black_list" + path + userId) != null;
    }

    public Boolean isDuplicateBlackList(String path, String username) {
        return redisTemplate.opsForValue().get("duplicate_black_list" + path + username) != null;
    }
}
