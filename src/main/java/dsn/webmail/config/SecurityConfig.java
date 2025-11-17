package dsn.webmail.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final Environment environment;

    public SecurityConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        var authRequests = http
            .csrf(csrf -> csrf.disable()) // CSRF 완전 비활성화
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                // API 엔드포인트
                .requestMatchers("/api/signup/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // 웹 페이지 엔드포인트
                .requestMatchers("/signup/**").permitAll()
                .requestMatchers("/", "/index").permitAll()
                
                // 정적 리소스
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                .requestMatchers("/webjars/**").permitAll()
            );

        // H2 console only in dev/test profiles
        boolean isDev = java.util.Arrays.asList(environment.getActiveProfiles()).contains("dev") ||
                       java.util.Arrays.asList(environment.getActiveProfiles()).contains("test") ||
                       environment.getActiveProfiles().length == 0; // default profile
        
        if (isDev) {
            authRequests.authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()
            );
            http.headers(headers -> headers.frameOptions().disable());
        }
        
        authRequests.authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
        
        return http.build();
    }
}