# ===== 1단계: jar 빌드 =====
FROM gradle:8.7-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle clean build -x test

# ===== 2단계: 실행 이미지 =====
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 빌더에서 jar 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 로그 디렉토리 생성 (application-prod.yml의 logs/application.log와 매칭)
RUN mkdir -p /app/logs && chmod 755 /app/logs

# 포트
EXPOSE 8080

# prod 프로필로 실행
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
