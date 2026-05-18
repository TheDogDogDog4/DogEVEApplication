package com.dog.evesystem.service.Impl;

import com.Dog.Doman.Result;
import com.Dog.Doman.ResultEnum;
import com.Dog.Exception.BusinessException;
import com.Dog.Utils.RedisUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dog.evesystem.dao.EVECharacterMapper;
import com.dog.evesystem.dao.UserMapper;
import com.Dog.Doman.dto.postgreSQL.PgEVECharacter;
import com.Dog.Doman.dto.resp.EVECharacterResp;
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

    private static final long DUPLICATE_EXPIRE = 5;

    @Override
    public RedirectView loginESI(Long userId) {
        log.info("ESI 登录业务 | {}", userId);
        if (userId == null) {
            log.warn("登录，用户不存在");
            throw new BusinessException(ResultEnum.USERNAME_NOT_EXITS);
        }

        try {
            // 防 CSRF 攻击
            String state = UUID.randomUUID().toString();
            redisUtil.set("oauth_state:" + state, userId, 600);

            // 发送 http 请求
            String url = authUrl + "?response_type=code" + "&client_id=" + clientId + "&redirect_uri=" + callbackUrl + "&scope=publicData" + "&state=" + state;

            // 放重复提交
            redisUtil.setDuplicateBlackList("/eve/login", userId, DUPLICATE_EXPIRE);
            log.info("ESI 登录防重复提交已写入 | {}", userId);

            return new RedirectView(url);
        } catch (Exception e) {
            log.warn("登录 API请求失败 | {}", authUrl);
            throw new BusinessException(ResultEnum.ESI_AUTH_REDIRECT_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> callbackESI(String code, String state) {
        log.info("callback 业务");

        Object object =  redisUtil.get("oauth_state:" + state);

        // 防 CSRF 攻击
        if (object == null) {
            log.warn("识别码不合规 | {}", state);
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
            log.warn("token API请求失败 | {}", tokenUrl);
            throw new BusinessException(ResultEnum.ESI_TOKEN_REQUEST_ERROR);
        }

        if (response == null || !response.containsKey("access_token") || response.get("access_token") == null) {
            log.warn("ESI token 获取失败 callback");
            throw new BusinessException(ResultEnum.ESI_TOKEN_INVALID_ERROR);
        }

        String accessToken = (String) response.get("access_token");
        redisUtil.set("user_esi_access_token_" + userId, accessToken, accessTokenExpire);
        String refreshToken = (String) response.get("refresh_token");
        redisUtil.set("user_esi_refresh_token_" + userId, refreshToken, refreshTokenExpire);

        User user = userMapper.selectById(userId);

        user.setEsiRefreshToken(refreshToken);
        userMapper.updateById(user);

        // 放重复提交
        redisUtil.setDuplicateBlackList("/eve/callback", userId, DUPLICATE_EXPIRE);
        log.info("ESI CALLBACK 防重复提交已写入 | {}", userId);

        return ResponseEntity.ok(Result.success(accessToken));
    }

    @Override
    public Result<Void> refreshESIToken(Long userId) {
        log.info("ESI token 刷新业务 | {}", userId);

        // 发送 http 请求
        User user = userMapper.selectById(userId);

        if (user == null) {
            log.warn("用户不存在");
            throw new BusinessException(ResultEnum.USERNAME_NOT_EXITS);
        }

        String refreshToken = user.getEsiRefreshToken();

        if (refreshToken == null) {
            log.warn("长期 token 不存在");
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
            log.warn("token 刷新 API请求失败 | {}", tokenUrl);
            throw new BusinessException(ResultEnum.ESI_TOKEN_REFRESH_ERROR);
        }

        if (response == null || !response.containsKey("access_token") || response.get("access_token") == null) {
            log.warn("ESI token 获取失败");
            throw new BusinessException(ResultEnum.ESI_TOKEN_INVALID_ERROR);
        }

        // 存入 redis
        String accessToken = (String) response.get("access_token");
        String newRefreshToken = (String) response.get("refresh_token");
        redisUtil.set("user_esi_access_token_" + userId, accessToken, accessTokenExpire);
        redisUtil.set("user_esi_refresh_token_" + userId, newRefreshToken, refreshTokenExpire);
        user.setEsiRefreshToken(newRefreshToken);
        userMapper.updateById(user);

        // 放重复提交
        redisUtil.setDuplicateBlackList("/eve/refresh", userId, DUPLICATE_EXPIRE);
        log.info("ESI token 刷新防重复提交已写入 | {}", userId);

        // 记录日志, 返回响应
        log.info("【刷新成功】");
        return Result.success();
    }

    @Override
    public Result<EVECharacterResp> characterInfo(Long userId) {
        // 记录日志
        log.info("获取游戏角色信息 | {}", userId);

        // 发送 http 请求
        HttpHeaders headers = new HttpHeaders();
        String accessToken = (String) redisUtil.get("user_esi_access_token_" + userId);

        if (accessToken == null) {
            log.warn("token 失效");
            throw new BusinessException(ResultEnum.ESI_TOKEN_INVALID_ERROR);
        }

        headers.setBearerAuth(accessToken);

        // 接收响应
        ResponseEntity<PgEVECharacter> response;
        try {
            response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, new HttpEntity<>(headers), PgEVECharacter.class);
        } catch (RestClientException e) {
            log.warn("角色信息获取 API请求失败 | {}", tokenUrl);
            throw new BusinessException(ResultEnum.ESI_USER_INFO_ERROR);
        }

        if (response.getBody() == null) {
            log.warn("角色信息获取失败");
            throw new BusinessException(ResultEnum.ESI_USER_INFO_ERROR);
        }

        // 角色存入数据库
        PgEVECharacter newEVECharacter = response.getBody();
        newEVECharacter.setUserId(userId);

        PgEVECharacter oldEVECharacter = eveCharacterMapper.selectOne(new QueryWrapper<PgEVECharacter>().eq("character_name", newEVECharacter.getCharacterName()));

        if (oldEVECharacter == null) {
            eveCharacterMapper.insert(newEVECharacter);
        } else {
            newEVECharacter.setCharacterId(oldEVECharacter.getCharacterId());
            eveCharacterMapper.updateById(newEVECharacter);
        }

        EVECharacterResp eveCharacterResp = new EVECharacterResp();
        BeanUtils.copyProperties(newEVECharacter, eveCharacterResp);

        // 放重复提交
        redisUtil.setDuplicateBlackList("/eve/info", userId, DUPLICATE_EXPIRE);
        log.info("ESI 角色信息获取防重复提交已写入 | {}", userId);

        // 记录日志, 返回响应
        log.info("【查询成功】");
        return Result.success(eveCharacterResp);
    }
}
