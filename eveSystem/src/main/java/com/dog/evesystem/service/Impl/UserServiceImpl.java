package com.dog.evesystem.service.Impl;

import com.dog.evesystem.dao.EVECharacterMapper;
import com.dog.evesystem.dao.UserMapper;
import com.dog.evesystem.doman.Result;
import com.dog.evesystem.doman.po.EVECharacter;
import com.dog.evesystem.doman.po.User;
import com.dog.evesystem.doman.vo.resp.EVECharacterResp;
import com.dog.evesystem.service.UserService;
import com.dog.evesystem.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
        // 记录日志
        log.info("【ESI 登录】{}", userId);

        // 防 CSRF 攻击
        String state = UUID.randomUUID().toString();
        redisUtil.set("oauth_state:" + state, userId, 600);

        // 发送 http 请求
        String url = authUrl + "?response_type=code" + "&client_id=" + clientId + "&redirect_uri=" + callbackUrl + "&scope=publicData" + "&state=" + state;
        //记录日志, 并跳转
        log.info("调用成功, 跳转");
        return new RedirectView(url);
    }

    @Override
    public ResponseEntity<?> callbackESI(String code, String state) {
        // 记录日志
        log.info("【callback 开始】{} {}", code, state);
        // 防 CSRF 攻击
        if (redisUtil.get("oauth_state:" + state) == null) {
            log.warn("【识别码错误】");
            return ResponseEntity.status(500).build();
        }

        Long userId = (Long) redisUtil.get("oauth_state:" + state);

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

        Map response = restTemplate.postForObject(tokenUrl, request, Map.class);

        if (response == null || !response.containsKey("access_token")) {
            log.warn("【token 无效】{}", request);
            return ResponseEntity.status(500).body("error");
        }

        String accessToken = (String) response.get("access_token");
        redisUtil.set("user_esi_access_token_" + userId, accessToken, accessTokenExpire);
        String refreshToken = (String) response.get("refresh_token");
        redisUtil.set("user_esi_refresh_token_" + userId, refreshToken, refreshTokenExpire);
        User user = userMapper.selectById(userId);
        user.setEsiRefreshToken(refreshToken);
        userMapper.updateById(user);

        // 记录日志, 返回响应
        log.info("ESI 登录成功");
        return ResponseEntity.ok(accessToken);
    }

    @Override
    public ResponseEntity<Result<Void>> refreshESIToken(Long userId) {
        // 记录日志
        log.info("【esi token 刷新】");

        // 发送 http 请求
        User user = userMapper.selectById(userId);
        String refreshToken = user.getEsiRefreshToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "refresh_token");
        map.add("refresh_token", refreshToken);
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        // 接收响应
        Map response = restTemplate.postForObject(tokenUrl, request, Map.class);

        if (response == null || !response.containsKey("access_token")) {
            log.warn("【刷新失败】");
            return ResponseEntity.status(500).body(Result.error());
        }

        // 存入 redis
        String accessToken = (String) response.get("access_token");
        redisUtil.set("user_esi_access_token_" + userId, accessToken, accessTokenExpire);
        redisUtil.set("user_refresh_token_" + userId, refreshToken, refreshTokenExpire);
        refreshToken = (String) response.get("refresh_token");
        user.setEsiRefreshToken(refreshToken);
        userMapper.updateById(user);

        // 记录日志, 返回响应
        log.info("【刷新成功】");
        return ResponseEntity.ok(Result.success());
    }

    @Override
    public ResponseEntity<Result<EVECharacterResp>> characterInfo(Long userId) {
        // 记录日志
        log.info("【获取角色】");

        // 发送 http 请求
        HttpHeaders headers = new HttpHeaders();
        String accessToken = (String) redisUtil.get("user_esi_access_token_" + userId);

        if (accessToken == null) {
            log.warn("【token 无效】");
            return ResponseEntity.status(500).body(Result.error());
        }

        headers.setBearerAuth(accessToken);

        // 接收响应
        ResponseEntity<EVECharacter> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, new HttpEntity<>(headers), EVECharacter.class);

        if (response.getBody() == null) {
            log.warn("【角色不存在】");
            return ResponseEntity.status(500).body(Result.error());
        }

        // 角色存入数据库
        EVECharacter eveCharacter = response.getBody();
        eveCharacter.setUserId(userId);
        eveCharacterMapper.insert(eveCharacter);

        EVECharacterResp eveCharacterResp = new EVECharacterResp();
        BeanUtils.copyProperties(eveCharacter, eveCharacterResp);

        // 记录日志, 返回响应
        log.info("【查询成功】");
        return ResponseEntity.ok(Result.success(eveCharacterResp));
    }
}
