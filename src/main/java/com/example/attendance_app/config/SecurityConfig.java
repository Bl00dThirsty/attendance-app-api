package com.example.attendance_app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;

@Configuration
public class SecurityConfig {

    private final String jwtSecret;
    private final List<String> corsAllowedOrigins;

    public SecurityConfig(
        @Value("${app.security.jwt.secret}") String jwtSecret,
        @Value("#{'${app.security.cors.allowed-origins:http://localhost:4200,http://127.0.0.1:4200}'.split(',')}")
        List<String> corsAllowedOrigins
    ) {
        this.jwtSecret = jwtSecret;
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/h2-console/**"
                ).permitAll()
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/employees/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/employees/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/employees/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/employees/**").hasAnyRole("ADMIN", "HR")
                .requestMatchers(HttpMethod.POST, "/api/departments/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/departments/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/departments/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/departments/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/positions/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/positions/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/positions/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/positions/**").hasAnyRole("ADMIN", "HR")
                .requestMatchers(HttpMethod.POST, "/api/sites/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/sites/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/sites/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/sites/**").hasAnyRole("ADMIN", "HR", "EMPLOYEE")
                .requestMatchers(HttpMethod.POST, "/api/attendance/check-in").hasAnyRole("ADMIN", "HR", "EMPLOYEE")
                .requestMatchers(HttpMethod.GET, "/api/attendance/**").hasAnyRole("ADMIN", "HR")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

        return http.build();
    }

    @Bean
    SecretKey jwtSecretKey() {
        byte[] decoded = Base64.getDecoder().decode(jwtSecret);
        return new SecretKeySpec(decoded, "HmacSHA256");
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey secretKey) {
        return NimbusJwtDecoder.withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    }

    @Bean
    Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return authenticationConverter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsAllowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}