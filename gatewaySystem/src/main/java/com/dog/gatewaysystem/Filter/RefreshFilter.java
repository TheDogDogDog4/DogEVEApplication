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

@Slf4j
@Component
public class RefreshFilter implements GlobalFilter, Ordered {

    @Resource
    private RedisUtil redisUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();

        log.info("token 刷新过滤器 | {} {}", method, path);

        // 只拦截 token 刷新路径
        if (path.equals("/auth/refresh")) {
            log.info("白名单放行 | {} {}", method, path);
            return chain.filter(exchange);
        }

        // 获取 refreshToken
        String firstToken = request.getHeaders().getFirst("Authorization");
        if (firstToken == null || !firstToken.startsWith("Bearer ")) {
            log.warn("请求未带正确请求头 | {} {} {}", method, path, firstToken);
            return unauthorized(exchange);
        }

        String refreshToken = firstToken.substring(7);

        // 检验 refreshToken 有效性, 排查是否进入黑名单
        if (!JwtUtil.verify(refreshToken) || redisUtil.isBlackList(refreshToken)) {
            log.warn("token 不合规或已失效 | {} {}", method, path);
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
        return 50;
    }
}
