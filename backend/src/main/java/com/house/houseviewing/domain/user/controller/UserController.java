package com.house.houseviewing.domain.user.controller;

import com.house.houseviewing.domain.user.dto.response.UserMeResponse;
import com.house.houseviewing.domain.user.dto.request.UserFindIdRequest;
import com.house.houseviewing.domain.user.dto.response.UserFindIdResponse;
import com.house.houseviewing.domain.auth.model.CustomUserDetails;
import com.house.houseviewing.domain.user.dto.request.UserResetPasswordRequest;
import com.house.houseviewing.domain.user.dto.request.UserVerifyPasswordRequest;
import com.house.houseviewing.domain.user.dto.request.UserRegisterRequest;
import com.house.houseviewing.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody UserRegisterRequest request){
        userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> me(@AuthenticationPrincipal CustomUserDetails userDetails){
        UserMeResponse result = userService.me(userDetails.getUserId());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/find-id")
    public ResponseEntity<UserFindIdResponse> findLoginId(@Valid @RequestBody UserFindIdRequest request){
        UserFindIdResponse result = userService.findLoginId(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/password/verify")
    public ResponseEntity<String> verifyPassword(@Valid @RequestBody UserVerifyPasswordRequest request){
        String result = userService.passwordVerify(request);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody UserResetPasswordRequest request){
        userService.passwordReset(request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal CustomUserDetails userDetails){
        userService.delete(userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }
}
