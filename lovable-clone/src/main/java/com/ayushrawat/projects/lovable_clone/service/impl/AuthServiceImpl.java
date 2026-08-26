package com.ayushrawat.projects.lovable_clone.service.impl;

import com.ayushrawat.projects.lovable_clone.dto.auth.AuthResponse;
import com.ayushrawat.projects.lovable_clone.dto.auth.LoginRequest;
import com.ayushrawat.projects.lovable_clone.dto.auth.SignupRequest;
import com.ayushrawat.projects.lovable_clone.error.BadRequestException;
import com.ayushrawat.projects.lovable_clone.mapper.UserMapper;
import com.ayushrawat.projects.lovable_clone.repository.UserRepository;
import com.ayushrawat.projects.lovable_clone.security.AuthUtils;
import com.ayushrawat.projects.lovable_clone.service.AuthService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import com.ayushrawat.projects.lovable_clone.entity.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
public class AuthServiceImpl implements AuthService {

    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    AuthUtils authUtils;
    AuthenticationManager authenticationManager;

    @Override
    public AuthResponse signup(SignupRequest request) {
        userRepository.findByUsername(request.username())
                .ifPresent(user->{
                    throw new BadRequestException("User Already exist with username :" + request.username());
                }
                );
                User user = userMapper.toEntity(request);
                user.setPassword(passwordEncoder.encode(request.password()));
                user=userRepository.save(user);

        String token = authUtils.generateAccessToken(user);
        return new AuthResponse(token,userMapper.toUserProfileResponse(user));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        User user = (User) authentication.getPrincipal();

        String token = authUtils.generateAccessToken(user);
        return new AuthResponse(token,userMapper.toUserProfileResponse(user));

    }
    }

