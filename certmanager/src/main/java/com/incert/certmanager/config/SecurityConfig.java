package com.incert.certmanager.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;


@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final KeycloakJwtConverter keycloakJwtConverter;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private List<String> allowedOrigins;

    private static final String[] PUBLIC_ENDPOINTS = {
        "/actuator/health",
        "/actuator/info"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless API
            .csrf(AbstractHttpConfigurer::disable)

            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Stateless sessions
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // URL-level authorization
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()  // CORS preflight
                
                // HTTP-level RBAC rules
                .requestMatchers(HttpMethod.POST, "/api/v1/certificates/upload").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/certificates/fetch-url").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/certificates/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/certificates/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/certificates/**").hasAnyRole("ADMIN", "USER")
                
                .anyRequest().authenticated()
            )

            // Configure OAuth2 Resource Server to validate JWT Bearer tokens
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtConverter))
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
