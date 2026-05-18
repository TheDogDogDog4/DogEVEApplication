package com.Dog.Doman.dto.resp;

import lombok.Data;

// JWT 令牌响应类
@Data
public class JwtTokenResp {
    private String accessToken;
    private String refreshToken;
}
