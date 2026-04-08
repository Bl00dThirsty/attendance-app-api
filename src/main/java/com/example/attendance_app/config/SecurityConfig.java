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
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Configuration
public class SecurityConfig {

    private final String jwtSecret;
    private final String jwtIssuer;
    private final String jwtAudience;
    private final List<String> corsAllowedOrigins;
    private final List<String> corsAllowedMethods;
    private final List<String> corsAllowedHeaders;
    private final List<String> corsExposedHeaders;
    private final boolean corsAllowCredentials;
    private final long corsMaxAgeSeconds;
    private final boolean corsEnforceSecurePolicy;
    private final boolean swaggerUiEnabled;
    private final boolean apiDocsEnabled;
    private final boolean h2ConsoleEnabled;

    public SecurityConfig(
        @Value("${app.security.jwt.secret}") String jwtSecret,
        @Value("${app.security.jwt.issuer:cale-auth-service}") String jwtIssuer,
        @Value("${app.security.jwt.audience:attendance-app-api}") String jwtAudience,
        @Value("#{'${app.security.cors.allowed-origins}'.split(',')}")
        List<String> corsAllowedOrigins,
        @Value("#{'${app.security.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}'.split(',')}")
        List<String> corsAllowedMethods,
        @Value("#{'${app.security.cors.allowed-headers:Authorization,Content-Type,Accept,Origin}'.split(',')}")
        List<String> corsAllowedHeaders,
        @Value("#{'${app.security.cors.exposed-headers:Authorization}'.split(',')}")
        List<String> corsExposedHeaders,
        @Value("${app.security.cors.allow-credentials:true}") boolean corsAllowCredentials,
        @Value("${app.security.cors.max-age-seconds:3600}") long corsMaxAgeSeconds,
        @Value("${app.security.cors.enforce-secure-policy:false}") boolean corsEnforceSecurePolicy,
        @Value("${springdoc.swagger-ui.enabled:true}") boolean swaggerUiEnabled,
        @Value("${springdoc.api-docs.enabled:true}") boolean apiDocsEnabled,
        @Value("${spring.h2.console.enabled:false}") boolean h2ConsoleEnabled
    ) {
        this.jwtSecret = jwtSecret;
        this.jwtIssuer = jwtIssuer;
        this.jwtAudience = jwtAudience;
        this.corsAllowedOrigins = normalizeOrigins(corsAllowedOrigins);
        this.corsAllowedMethods = normalizeUpperValues(corsAllowedMethods);
        this.corsAllowedHeaders = normalizeValues(corsAllowedHeaders);
        this.corsExposedHeaders = normalizeValues(corsExposedHeaders);
        this.corsAllowCredentials = corsAllowCredentials;
        this.corsMaxAgeSeconds = corsMaxAgeSeconds;
        this.corsEnforceSecurePolicy = corsEnforceSecurePolicy;
        this.swaggerUiEnabled = swaggerUiEnabled;
        this.apiDocsEnabled = apiDocsEnabled;
        this.h2ConsoleEnabled = h2ConsoleEnabled;

        validateCorsConfiguration();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> {
                if (swaggerUiEnabled || apiDocsEnabled) {
                    authorize.requestMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                    ).permitAll();
                } else {
                    authorize.requestMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                    ).denyAll();
                }

                if (h2ConsoleEnabled) {
                    authorize.requestMatchers("/h2-console/**").permitAll();
                } else {
                    authorize.requestMatchers("/h2-console/**").denyAll();
                }

                authorize
                    .requestMatchers("/actuator/health/**").permitAll()
                    .requestMatchers("/actuator/**").hasRole("ADMIN")
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
                    .requestMatchers(HttpMethod.POST, "/api/attendance/**").hasAnyRole("ADMIN", "HR")
                    .requestMatchers(HttpMethod.PUT, "/api/attendance/**").hasAnyRole("ADMIN", "HR")
                    .requestMatchers(HttpMethod.PATCH, "/api/attendance/**").hasAnyRole("ADMIN", "HR")
                    .requestMatchers(HttpMethod.DELETE, "/api/attendance/**").hasAnyRole("ADMIN", "HR")
                    .requestMatchers(HttpMethod.GET, "/api/attendance/**").hasAnyRole("ADMIN", "HR")
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().permitAll();
            })
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .headers(headers -> {
                if (h2ConsoleEnabled) {
                    headers.frameOptions(frameOptions -> frameOptions.sameOrigin());
                } else {
                    headers.frameOptions(frameOptions -> frameOptions.deny());
                }
            });

        return http.build();
    }

    @Bean
    SecretKey jwtSecretKey() {
        byte[] decoded = Base64.getDecoder().decode(jwtSecret);
        return new SecretKeySpec(decoded, "HmacSHA256");
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey secretKey) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();

        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(jwtIssuer);
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<>("aud", aud -> {
            if (aud instanceof String audienceValue) {
                return Objects.equals(audienceValue, jwtAudience);
            }
            if (aud instanceof List<?> audienceValues) {
                return audienceValues.stream().anyMatch(value -> Objects.equals(String.valueOf(value), jwtAudience));
            }
            return false;
        });

        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
        return jwtDecoder;
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
        configuration.setAllowedMethods(corsAllowedMethods);
        configuration.setAllowedHeaders(corsAllowedHeaders);
        if (!corsExposedHeaders.isEmpty()) {
            configuration.setExposedHeaders(corsExposedHeaders);
        }
        configuration.setAllowCredentials(corsAllowCredentials);
        configuration.setMaxAge(corsMaxAgeSeconds);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    private void validateCorsConfiguration() {
        if (corsAllowedOrigins.isEmpty()) {
            throw new IllegalStateException("app.security.cors.allowed-origins must contain at least one origin");
        }
        if (corsAllowedMethods.isEmpty()) {
            throw new IllegalStateException("app.security.cors.allowed-methods must contain at least one method");
        }
        if (corsAllowedHeaders.isEmpty()) {
            throw new IllegalStateException("app.security.cors.allowed-headers must contain at least one header");
        }
        if (corsMaxAgeSeconds < 0) {
            throw new IllegalStateException("app.security.cors.max-age-seconds must be >= 0");
        }
        if (corsAllowedOrigins.stream().anyMatch(origin -> origin.contains("*"))) {
            throw new IllegalStateException("Wildcard origins are not allowed in app.security.cors.allowed-origins");
        }
        if (corsAllowCredentials && corsAllowedOrigins.stream().anyMatch(origin -> "*".equals(origin))) {
            throw new IllegalStateException("Wildcard origins are incompatible with allow-credentials=true");
        }

        if (corsEnforceSecurePolicy) {
            if (corsAllowedMethods.stream().anyMatch(method -> method.contains("*"))) {
                throw new IllegalStateException("Wildcard methods are not allowed when secure CORS policy is enabled");
            }
            if (corsAllowedHeaders.stream().anyMatch(header -> header.contains("*"))) {
                throw new IllegalStateException("Wildcard headers are not allowed when secure CORS policy is enabled");
            }
            for (String origin : corsAllowedOrigins) {
                validateProductionOrigin(origin);
            }
        }
    }

    private void validateProductionOrigin(String origin) {
        URI uri;
        try {
            uri = new URI(origin);
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Invalid CORS origin: " + origin, exception);
        }

        String scheme = uri.getScheme();
        if (scheme == null || !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalStateException(
                "Only HTTPS origins are allowed when app.security.cors.enforce-secure-policy=true: " + origin
            );
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalStateException("CORS origin host is missing: " + origin);
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if ("localhost".equals(normalizedHost) || "127.0.0.1".equals(normalizedHost) || "::1".equals(normalizedHost)) {
            throw new IllegalStateException(
                "Localhost origins are forbidden when app.security.cors.enforce-secure-policy=true: " + origin
            );
        }
        if (uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalStateException("CORS origin must not contain userinfo, query, or fragment: " + origin);
        }
        if (uri.getPath() != null && !uri.getPath().isBlank() && !"/".equals(uri.getPath())) {
            throw new IllegalStateException("CORS origin must not include a path: " + origin);
        }
    }

    private List<String> normalizeValues(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
            .map(value -> value == null ? "" : value.trim())
            .filter(value -> !value.isEmpty())
            .distinct()
            .toList();
    }

    private List<String> normalizeUpperValues(List<String> values) {
        return normalizeValues(values).stream()
            .map(value -> value.toUpperCase(Locale.ROOT))
            .toList();
    }

    private List<String> normalizeOrigins(List<String> values) {
        return normalizeValues(values).stream()
            .map(value -> {
                String normalized = value;
                while (normalized.endsWith("/")) {
                    normalized = normalized.substring(0, normalized.length() - 1);
                }
                return normalized;
            })
            .toList();
    }
}
