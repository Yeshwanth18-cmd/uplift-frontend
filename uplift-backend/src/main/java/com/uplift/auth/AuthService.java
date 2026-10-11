package com.uplift.auth;

import com.uplift.auth.dto.AuthResponse;
import com.uplift.auth.dto.LoginRequest;
import com.uplift.auth.dto.RegisterRequest;

import com.uplift.entity.User;
import com.uplift.entity.Role;
import com.uplift.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public AuthResponse register(
            RegisterRequest request
    ) {
        if (request.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Administrator accounts cannot be created through public registration."
            );
        }

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already registered."
            );
        }

        User user = new User(
                request.getName().trim(),
                email,
                passwordEncoder.encode(
                        request.getPassword()
                ),
                request.getRole()
        );

        User savedUser =
                userRepository.save(user);

        String token =
                jwtService.generateToken(savedUser);

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }

    public AuthResponse login(
            LoginRequest request
    ) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().trim().toLowerCase(Locale.ROOT),
                        request.getPassword()
                )
        );

        User user =
                userRepository.findByEmail(
                        request.getEmail().trim().toLowerCase(Locale.ROOT)
                ).orElseThrow(
                        () -> new RuntimeException(
                                "User not found"
                        )
                );

        String token =
                jwtService.generateToken(user);

        return new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}