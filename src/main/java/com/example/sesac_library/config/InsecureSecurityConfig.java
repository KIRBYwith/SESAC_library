package com.example.sesac_library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile({"prod", "local"}) // ← prod와 local에서 동일한 취약 설정 사용
public class InsecureSecurityConfig {

    @Bean
    SecurityFilterChain insecureFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())               // CSRF 보호 해제
                .headers(headers -> headers.disable())      // 보안 헤더 모두 제거
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()               // 모든 요청 허용
                );
        return http.build();
    }
}
