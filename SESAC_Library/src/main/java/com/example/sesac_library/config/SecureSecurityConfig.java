package com.example.sesac_library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("secure")   // secure 프로필일 때만 이 설정 사용
public class SecureSecurityConfig {

    @Bean
    SecurityFilterChain secureFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(Customizer.withDefaults())           // CSRF 기본 활성화
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                        .frameOptions(frame -> frame.sameOrigin())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true)
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        // 정적 자원과 일부 API는 공개
                        .requestMatchers(
                                "/", "/index.html", "/login.html", "/books.html",
                                "/css/**", "/js/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/books/**").permitAll()

                        // 관리자 영역 보호
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                )
                .formLogin(Customizer.withDefaults())
                .logout(Customizer.withDefaults());

        return http.build();
    }
}
