package com.example.whatsappOrdering.service;

import com.example.whatsappOrdering.config.AppProperties;
import com.example.whatsappOrdering.dto.auth.AuthResponse;
import com.example.whatsappOrdering.dto.auth.LoginRequest;
import com.example.whatsappOrdering.dto.auth.SignupRequest;
import com.example.whatsappOrdering.entity.User;
import com.example.whatsappOrdering.entity.enums.UserRole;
import com.example.whatsappOrdering.exception.BusinessException;
import com.example.whatsappOrdering.exception.UnauthorizedException;
import com.example.whatsappOrdering.repository.UserRepository;
import com.example.whatsappOrdering.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AppProperties appProperties;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already registered: " + request.email());
        }
        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.OWNER)
                .build();
        user = userRepository.save(user);
        String token = jwtUtil.generateToken(user);
        return new AuthResponse(token, appProperties.getJwt().getExpirationMs(),
                user.getId(), user.getName(), user.getEmail());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        String token = jwtUtil.generateToken(user);
        return new AuthResponse(token, appProperties.getJwt().getExpirationMs(),
                user.getId(), user.getName(), user.getEmail());
    }
}
