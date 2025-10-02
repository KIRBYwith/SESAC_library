package com.example.sesac_library.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /files/** URL을 C://upload/ 폴더와 매핑
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:///C:/upload/")
                .setCachePeriod(3600); // 1시간 캐시

        System.out.println("✅ 파일 경로 매핑: /files/** -> C:/upload/");
    }
}