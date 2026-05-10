package com.dog.gatewaysystem.Filter;

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
public class DuplicateFilter implements GlobalFilter, Ordered {

    @Resource
    private RedisUtil redisUtil;

    private static final List<String> WHITE_LIST = Arrays.asList(
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();

        // 白名单放行
        if (WHITE_LIST.contains(path)) {
            return chain.filter(exchange);
        }

        if (path.equals("/auth/login")) {
            String username = request.getHeaders().getFirst("username");

            // 查询是否为重复提交
            if (redisUtil.isDuplicateBlackList(path, username)) {
                return unauthorized(exchange);
            }
        } else {
            String userIdStr = request.getHeaders().getFirst("userId");
            Long userId = null;
            if (userIdStr != null) {
                userId = Long.parseLong(userIdStr);
            }

            // 查询是否为重复提交
            if (redisUtil.isDuplicateBlackList(path, userId)) {
                return unauthorized(exchange);
            }
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

    // 401 返回
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        ServerHttpRequest request = exchange.getRequest();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);

        // 记录重复提交日志
        log.warn("【重复提交】{} {} | {}", request.getMethod(), request.getPath(), HttpStatus.UNAUTHORIZED);

        // 响应结果
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -40;
    }
}
