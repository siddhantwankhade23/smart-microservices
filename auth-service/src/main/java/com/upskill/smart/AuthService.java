package com.upskill.smart;

import com.upskill.smart.dto.LoginRequest;
import com.upskill.smart.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public void registerUser(LoginRequest loginRequest) {
        UserEntity user = new UserEntity();
        user.setUsername(loginRequest.username());

        repository.findByUsername(user.getUsername()).ifPresent(existingUser -> {
            throw new RuntimeException("User already exists");
        });

        user.setRole(loginRequest.role());
        user.setPassword(passwordEncoder.encode(loginRequest.password()));
        repository.save(user);
    }

    public LoginResponse login(LoginRequest loginRequest) {

        UserEntity user = repository.findByUsername(loginRequest.username())
                .orElseThrow(() -> new RuntimeException("User does not exist"));

        boolean isPasswordCorrect = passwordEncoder.matches(loginRequest.password(), user.getPassword());

        if (!isPasswordCorrect) {
            throw new RuntimeException("Password is incorrect");
        }

        return new LoginResponse(jwtService.generateToken(user.getUsername(), user.getRole()));
    }

}
