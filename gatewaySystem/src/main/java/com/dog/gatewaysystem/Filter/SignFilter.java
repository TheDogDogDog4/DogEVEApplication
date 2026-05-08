package com.dog.gatewaysystem.Filter;

import com.dog.gatewaysystem.Utils.RedisUtil;
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

@Slf4j
@Component
public class SignFilter implements GlobalFilter, Ordered {

    @Resource
    private RedisUtil redisUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        HttpMethod method = request.getMethod();
        String path = request.getPath().value();

        // 其他路径放行
        if (!path.equals("/auth/login")) {
            return chain.filter(exchange);
        }

        String username = request.getHeaders().getFirst("username");

        // 检查是否次数过多
        if (redisUtil.isUsernameBlackList(username) >= 3) {
            return unauthorized(exchange);
        }

        return chain
                .filter(exchange.mutate().build())
                .then(Mono.fromRunnable(() -> {
                    HttpStatusCode status = response.getStatusCode();

                    // 记录成功日志
                    if (status == HttpStatus.OK) {
                        log.info("【正常】{} {} | {}", method, path, HttpStatus.OK);
                    }
                }));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        ServerHttpRequest request = exchange.getRequest();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);

        // 记录 401 登录次数过多错误日志
        log.warn("【尝试次数过多】{} {} | {}", request.getMethod(), request.getPath(), HttpStatus.UNAUTHORIZED);

        // 响应结果
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
