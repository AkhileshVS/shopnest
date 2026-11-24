package com.akhilesh.shopnest.services;

import com.akhilesh.shopnest.dtos.AuthResponse;
import com.akhilesh.shopnest.dtos.LoginRequest;
import com.akhilesh.shopnest.dtos.SignupRequest;
import com.akhilesh.shopnest.exceptions.BadRequestException;
import com.akhilesh.shopnest.models.Role;
import com.akhilesh.shopnest.models.User;
import com.akhilesh.shopnest.repositories.UserRepository;
import com.akhilesh.shopnest.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse signup(SignupRequest request) {
        String email = request.email().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already registered");
        }
        User user = new User();
        user.setName(request.name());
        user.setEmail(email);
        user.setPhone(request.phone());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.CUSTOMER);
        user = userRepository.save(user);
        return toResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user), user.getId(), user.getEmail(), user.getRole().name());
    }
}
