package com.dog.gatewaysystem.Filter;

import com.Dog.Utils.JwtUtil;
import com.Dog.Utils.RedisUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class RefreshFilter implements GlobalFilter, Ordered {

    @Resource
    private RedisUtil redisUtil;

    private static final List<String> BLACK_LIST = Arrays.asList(
            "/auth/refresh"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();

        // 不在黑名单直接放行
        if (!BLACK_LIST.contains(path)) {
            return chain.filter(exchange);
        }

        // 获取 refreshToken
        String firstToken = request.getHeaders().getFirst("Authorization");
        if (firstToken == null || !firstToken.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        String refreshToken = firstToken.substring(7);

        // 检验 refreshToken 有效性, 排查是否进入黑名单
        if (!JwtUtil.verify(refreshToken) || redisUtil.isBlackList(refreshToken)) {
            return unauthorized(exchange);
        }

        // 解析 userId, 传给下游
        Long userId = JwtUtil.getUserId(refreshToken);
        ServerHttpRequest newRequest = request.mutate()
                .header("userId", userId.toString())
                .build();

        // 向下游传递
        return chain
                .filter(exchange.mutate().request(newRequest).build())
                .then(Mono.fromRunnable(() -> {
                    HttpStatusCode status = response.getStatusCode();

                    // 记录成功日志
                    if (status == HttpStatus.OK) {
                        log.info("【正常】{} {} | {}", method, path, HttpStatus.OK);
                    }
                }));
    }

    // 401 返回
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        ServerHttpRequest request = exchange.getRequest();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);

        // 记录重复提交日志
        log.warn("【Token 失效】{} {} | {}", request.getMethod(), request.getPath(), HttpStatus.UNAUTHORIZED);

        // 响应结果
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
