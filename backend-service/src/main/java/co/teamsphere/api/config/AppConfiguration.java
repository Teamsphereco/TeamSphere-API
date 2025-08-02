package co.teamsphere.api.config;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import co.teamsphere.api.config.properties.AppProperties;
import co.teamsphere.api.config.properties.Argon2Properties;
import co.teamsphere.api.config.properties.JwtProperties;

@Configuration
public class AppConfiguration {
    private final AppProperties appProperties;

    private final PublicKey publicKey;

    private final JwtProperties jwtProperties;

    public AppConfiguration(AppProperties appProperties,
                            PublicKey publicKey,
                            JwtProperties jwtProperties) {
        this.appProperties = appProperties;
        this.publicKey = publicKey;
        this.jwtProperties = jwtProperties;
    }

    @Bean
    public SecurityFilterChain securityAppConfig(HttpSecurity http) throws Exception {
        http
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(new JWTTokenFilter(publicKey, jwtProperties), UsernamePasswordAuthenticationFilter.class)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(Argon2Properties argon2Properties) {
        return new Argon2PasswordEncoder(
            argon2Properties.getSaltLength(), // size in bytes for salting length
            argon2Properties.getHashLength(), // size in bytes for hashing length
            argon2Properties.getParallelism(), // number of threads (we only need 1 in java apparetly)
            argon2Properties.getMemoryCost(), // 64 mb in kb
            argon2Properties.getIterations() // number of iterations
        );
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return request -> {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(appProperties.getAllowedOrigins());
            configuration.setAllowedMethods(Collections.singletonList("*"));
            configuration.setAllowCredentials(true);
            configuration.setAllowedHeaders(Collections.singletonList("*"));
            configuration.setExposedHeaders(Arrays.asList("Authorization"));
            configuration.setMaxAge(3600L);
            return configuration;
        };
    }
}
