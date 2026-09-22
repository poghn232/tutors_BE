package com.giasuhq.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/api/auth/send-register-otp",
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/google",
                        "/api/auth/forgot-password",
                        "/api/auth/verify-otp",
                        "/api/auth/reset-password",
                        "/api/health",
                        "/error",
                        "/h2-console/**"
                ).permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/files/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/payments/vnpay/callback", "/api/payments/vnpay/ipn").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/payments/sepay/webhook").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/tutors/admin/all").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/tutors/*/verification").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/tutors").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/subjects").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/tutors/**", "/api/subjects/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/files/**").authenticated()
                .requestMatchers("/api/payments/**").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/classes").authenticated()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"success\":false,\"message\":\"Phiên đăng nhập đã hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại.\",\"data\":null}");
                })
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
