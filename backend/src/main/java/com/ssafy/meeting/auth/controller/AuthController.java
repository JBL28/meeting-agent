package com.ssafy.meeting.auth.controller;

import com.ssafy.meeting.auth.dto.AuthResponse;
import com.ssafy.meeting.auth.dto.LoginRequest;
import com.ssafy.meeting.auth.dto.RegisterRequest;
import com.ssafy.meeting.auth.security.UserPrincipal;
import com.ssafy.meeting.auth.service.AuthService;
import com.ssafy.meeting.common.api.ApiResponse;
import com.ssafy.meeting.member.dto.MemberResponse;
import javax.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<MemberResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<MemberResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(authService.me(principal.getId()));
    }
}
