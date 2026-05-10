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

// 过滤器类
@Slf4j
@Component
public class TokenVerifyFilter implements GlobalFilter, Ordered {

    @Resource
    private RedisUtil redisUtil;

    // 白名单
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/auth/login",
            "/auth/register",
            "/auth/refresh"
    );


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();

        // 记录请求的类型和地址
        log.info("【网关请求】{} {}", method, path);

        // 白名单直接放行
        if (WHITE_LIST.contains(path)) {
            return chain.filter(exchange);
        }

        // 获取 accessToken
        String firstToken = request.getHeaders().getFirst("Authorization");
        if (firstToken == null || !firstToken.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        String accessToken = firstToken.substring(7);

        // 检验 accessToken 有效性, 排查是否进入黑名单
        if (!JwtUtil.verify(accessToken) || redisUtil.isBlackList(accessToken)) {
            return unauthorized(exchange);
        }

        // 解析 userId，传给下游
        Long userId = JwtUtil.getUserId(accessToken);
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

        // 记录 401 token 失效日志
        log.warn("【Token 无效】{} {} | {}", request.getMethod(), request.getPath(), HttpStatus.UNAUTHORIZED);

        // 响应结果
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -90;
    }
}
