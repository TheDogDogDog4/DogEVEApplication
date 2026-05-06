package com.dog.gatewaysystem.Filter;

import com.dog.gatewaysystem.Utils.IpUtil;
import com.dog.gatewaysystem.Utils.JwtUtil;
import com.dog.gatewaysystem.Utils.RedisUtil;
import com.dog.gatewaysystem.Utils.TokenBucketUtil;
import jakarta.annotation.Resource;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.Arrays;
import java.util.List;

// 过滤器类
@Component
public class AuthGlobalFilter implements GlobalFilter {

    @Resource
    private RedisUtil redisUtil;
    @Resource
    private TokenBucketUtil tokenBucketUtil;

    // 白名单
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/auth/login",
            "/auth/register",
            "/auth/logout",
            "/auth/refresh"
    );


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 令牌桶
        String ip = IpUtil.getRealIp(request);
        if (tokenBucketUtil.tryAcquireToken(ip)) {
            return chain.filter(exchange);
        }

        // 白名单直接放行
        if (WHITE_LIST.stream().anyMatch(path::contains)) {
            return chain.filter(exchange);
        }

        // 获取 token
        String token = request.getHeaders().getFirst("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        String realToken = token.substring(7);

        // 校验 JWT
        if (!JwtUtil.verify(realToken)) {
            return unauthorized(exchange);
        }

        // 校验 Redis 黑名单
        if (redisUtil.isBlackList(realToken)) {
            return unauthorized(exchange);
        }

        // 解析 userId，传给下游
        Long userId = JwtUtil.getUserId(realToken);
        ServerHttpRequest newRequest = request.mutate()
                .header("userId", userId.toString())
                .build();

        return chain.filter(exchange.mutate().request(newRequest).build());
    }

    // 401 返回
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }
}
