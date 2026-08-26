package com.ayushrawat.projects.lovable_clone.controller;

import com.ayushrawat.projects.lovable_clone.dto.auth.AuthResponse;
import com.ayushrawat.projects.lovable_clone.dto.auth.LoginRequest;
import com.ayushrawat.projects.lovable_clone.dto.auth.SignupRequest;
import com.ayushrawat.projects.lovable_clone.dto.auth.UserProfileResponse;
import com.ayushrawat.projects.lovable_clone.security.AuthUtils;
import com.ayushrawat.projects.lovable_clone.service.AuthService;
import com.ayushrawat.projects.lovable_clone.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AuthController {

    AuthService authService;
    UserService userService;
    AuthUtils authUtils;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody @Valid SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile() {
        Long userId = authUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.getProfile(userId));
    }

}
