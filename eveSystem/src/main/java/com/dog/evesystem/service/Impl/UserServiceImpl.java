package com.dog.evesystem.service.Impl;

import com.Dog.Doman.Result;
import com.Dog.Doman.ResultEnum;
import com.Dog.Exception.BusinessException;
import com.Dog.Utils.RedisUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dog.evesystem.dao.EVECharacterMapper;
import com.dog.evesystem.dao.UserMapper;
import com.dog.evesystem.doman.dto.EVECharacter;
import com.dog.evesystem.doman.dto.User;
import com.dog.evesystem.doman.vo.resp.EVECharacterResp;
import com.dog.evesystem.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private EVECharacterMapper eveCharacterMapper;

    @Value("${eve.sso.client-id}")
    private String clientId;
    @Value("${eve.sso.client-secret}")
    private String clientSecret;
    @Value("${eve.sso.callback-url}")
    private String callbackUrl;
    @Value("${eve.sso.auth-url}")
    private String authUrl;
    @Value("${eve.sso.token-url}")
    private String tokenUrl;
    @Value("${eve.sso.user-info-url}")
    private String userInfoUrl;
    @Value("${eve.expire.access-token}")
    private long accessTokenExpire;
    @Value("${eve.expire.refresh-token}")
    private long refreshTokenExpire;

    @Override
    public RedirectView loginESI(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultEnum.USERNAME_NOT_EXITS);
        }

        try {
            // 防 CSRF 攻击
            String state = UUID.randomUUID().toString();
            redisUtil.set("oauth_state:" + state, userId, 600);

            // 发送 http 请求
            String url = authUrl + "?response_type=code" + "&client_id=" + clientId + "&redirect_uri=" + callbackUrl + "&scope=publicData" + "&state=" + state;

            return new RedirectView(url);
        } catch (Exception e) {
            throw new BusinessException(ResultEnum.ESI_AUTH_REDIRECT_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> callbackESI(String code, String state) {

        Object object =  redisUtil.get("oauth_state:" + state);

        // 防 CSRF 攻击
        if (object == null) {
            throw new BusinessException(ResultEnum.STATE_INCORRECT);
        }

        Long userId = (Long) object;

        redisUtil.delete("oauth_state:" + state);

        // 发送 http 请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "authorization_code");
        map.add("code", code);
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("redirect_uri", callbackUrl);

        // 接收响应
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        Map<String, Object> response;
        try {
            response = restTemplate.postForObject(tokenUrl, request, Map.class);
        } catch (RestClientException e) {
            throw new BusinessException(ResultEnum.ESI_TOKEN_REQUEST_ERROR);
        }

        if (response == null || !response.containsKey("access_token") || response.get("access_token") == null) {
            throw new BusinessException(ResultEnum.ESI_TOKEN_INVALID_ERROR);
        }

        String accessToken = (String) response.get("access_token");
        redisUtil.set("user_esi_access_token_" + userId, accessToken, accessTokenExpire);
        String refreshToken = (String) response.get("refresh_token");
        redisUtil.set("user_esi_refresh_token_" + userId, refreshToken, refreshTokenExpire);

        User user = userMapper.selectById(userId);

        if (user == null) {
            throw new BusinessException(ResultEnum.USERNAME_NOT_EXITS);
        }

        user.setEsiRefreshToken(refreshToken);
        userMapper.updateById(user);

        return ResponseEntity.ok(Result.success(accessToken));
    }

    @Override
    public Result<Void> refreshESIToken(Long userId) {

        // 发送 http 请求
        User user = userMapper.selectById(userId);

        if (user == null) {
            throw new BusinessException(ResultEnum.USERNAME_NOT_EXITS);
        }

        String refreshToken = user.getEsiRefreshToken();

        if (refreshToken == null) {
            throw new BusinessException(ResultEnum.ESI_TOKEN_INVALID_ERROR);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "refresh_token");
        map.add("refresh_token", refreshToken);
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        // 接收响应
        Map<String, Object> response;
        try {
            response = restTemplate.postForObject(tokenUrl, request, Map.class);
        } catch (RestClientException e) {
            throw new BusinessException(ResultEnum.ESI_TOKEN_REFRESH_ERROR);
        }

        if (response == null || !response.containsKey("access_token") || response.get("access_token") == null) {
            throw new BusinessException(ResultEnum.ESI_TOKEN_INVALID_ERROR);
        }

        // 存入 redis
        String accessToken = (String) response.get("access_token");
        String newRefreshToken = (String) response.get("refresh_token");
        redisUtil.set("user_esi_access_token_" + userId, accessToken, accessTokenExpire);
        redisUtil.set("user_esi_refresh_token_" + userId, newRefreshToken, refreshTokenExpire);
        user.setEsiRefreshToken(newRefreshToken);
        userMapper.updateById(user);

        // 记录日志, 返回响应
        log.info("【刷新成功】");
        return Result.success();
    }

    @Override
    public Result<EVECharacterResp> characterInfo(Long userId) {
        // 记录日志
        log.info("【获取角色】");

        // 发送 http 请求
        HttpHeaders headers = new HttpHeaders();
        String accessToken = (String) redisUtil.get("user_esi_access_token_" + userId);

        if (accessToken == null) {
            throw new BusinessException(ResultEnum.ESI_TOKEN_INVALID_ERROR);
        }

        headers.setBearerAuth(accessToken);

        // 接收响应
        ResponseEntity<EVECharacter> response;
        try {
            response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, new HttpEntity<>(headers), EVECharacter.class);
        } catch (RestClientException e) {
            throw new BusinessException(ResultEnum.ESI_USER_INFO_ERROR);
        }

        if (response.getBody() == null) {
            throw new BusinessException(ResultEnum.ESI_USER_INFO_ERROR);
        }

        // 角色存入数据库
        EVECharacter newEVECharacter = response.getBody();
        newEVECharacter.setUserId(userId);

        EVECharacter oldEVECharacter = eveCharacterMapper.selectOne(new QueryWrapper<EVECharacter>().eq("character_name", newEVECharacter.getCharacterName()));

        if (oldEVECharacter == null) {
            eveCharacterMapper.insert(newEVECharacter);
        } else {
            newEVECharacter.setCharacterId(oldEVECharacter.getCharacterId());
            eveCharacterMapper.updateById(newEVECharacter);
        }

        EVECharacterResp eveCharacterResp = new EVECharacterResp();
        BeanUtils.copyProperties(newEVECharacter, eveCharacterResp);

        // 记录日志, 返回响应
        log.info("【查询成功】");
        return Result.success(eveCharacterResp);
    }
}
