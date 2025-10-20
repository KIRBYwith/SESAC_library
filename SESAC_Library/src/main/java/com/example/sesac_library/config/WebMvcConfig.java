package com.example.sesac_library.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 프로젝트 내부 경로로 파일 매핑
        String absolutePath = new File(uploadDir).getAbsolutePath() + "/";

        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + absolutePath)
                .setCachePeriod(3600);

        System.out.println("✅ 파일 경로 매핑: /files/** -> " + absolutePath);
    }
}