package com.taskflow.auth;

import com.taskflow.user.Role;
import com.taskflow.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

/** Extracts a bearer JWT from the request and populates the security context. */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public record AuthenticatedUser(UUID id, String email, Role role) {
        public boolean isAdmin() {
            return role == Role.ADMIN;
        }
    }

    public static AuthenticatedUser currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (AuthenticatedUser) auth.getPrincipal();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            jwtService.parse(token)
                    .filter(claims -> !jwtService.isExpired(claims))
                    .ifPresent(claims -> {
                        try {
                            UUID id = UUID.fromString(claims.getSubject());
                            userRepository.findById(id).ifPresent(user -> {
                                var principal = new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole());
                                var authToken = new UsernamePasswordAuthenticationToken(
                                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
                                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                SecurityContextHolder.getContext().setAuthentication(authToken);
                            });
                        } catch (RuntimeException ignored) {
                            // invalid subject/format -> continue unauthenticated
                        }
                    });
        }
        chain.doFilter(request, response);
    }
}
