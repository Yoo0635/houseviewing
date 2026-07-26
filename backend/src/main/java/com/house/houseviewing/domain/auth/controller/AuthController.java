package com.house.houseviewing.domain.auth.controller;

import com.house.houseviewing.domain.auth.dto.request.LoginRequest;
import com.house.houseviewing.domain.auth.dto.request.ReissueRequest;
import com.house.houseviewing.domain.auth.dto.response.LoginResponse;
import com.house.houseviewing.domain.auth.dto.response.ReissueResponse;
import com.house.houseviewing.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request){
        LoginResponse result = authService.login(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reissue")
    public ResponseEntity<ReissueResponse> reissue(@RequestBody ReissueRequest request){
        ReissueResponse result = authService.reissue(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorizationHeader){
        authService.logout(authorizationHeader);
        return ResponseEntity.ok().build();
    }
}
