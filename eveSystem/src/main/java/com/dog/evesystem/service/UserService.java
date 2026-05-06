package com.dog.evesystem.service;

import com.dog.evesystem.doman.Result;
import com.dog.evesystem.doman.vo.resp.EVECharacterResp;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.view.RedirectView;

public interface UserService {
    RedirectView loginESI(Long userId);
    ResponseEntity<?> callbackESI(String code, String state);
    ResponseEntity<Result<Void>> refreshESIToken(Long userId);
    ResponseEntity<Result<EVECharacterResp>> characterInfo(Long userId);
}
