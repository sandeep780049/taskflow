package com.taskflow.auth;

import com.taskflow.common.ApiException;
import com.taskflow.user.UserAccount;
import com.taskflow.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

record RegisterRequest(
        @jakarta.validation.constraints.Email @jakarta.validation.constraints.NotBlank String email,
        @jakarta.validation.constraints.Size(min = 8, max = 72, message = "password must be 8-72 characters")
        @jakarta.validation.constraints.NotBlank String password) {
}

record LoginRequest(
        @jakarta.validation.constraints.Email @jakarta.validation.constraints.NotBlank String email,
        @jakarta.validation.constraints.NotBlank String password) {
}

record AuthResponse(String token, UserResponse user) {
}

record UserResponse(java.util.UUID id, String email, String role) {
    public static UserResponse from(UserAccount user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole().name());
    }
}

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw ApiException.conflict("Email is already registered");
        }
        UserAccount user = users.save(new UserAccount(
                request.email().toLowerCase().trim(),
                passwordEncoder.encode(request.password()),
                com.taskflow.user.Role.USER));
        return new AuthResponse(jwtService.issue(user), UserResponse.from(user));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        UserAccount user = users.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> ApiException.badRequest("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.badRequest("Invalid email or password");
        }
        return new AuthResponse(jwtService.issue(user), UserResponse.from(user));
    }

    @GetMapping("/me")
    public UserResponse me() {
        var principal = JwtAuthFilter.currentUser();
        UserAccount user = users.findById(principal.id())
                .orElseThrow(() -> ApiException.notFound("User not found"));
        return UserResponse.from(user);
    }
}
