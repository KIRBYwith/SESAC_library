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

# 로그 디렉토리 생성
RUN mkdir -p /logs && chmod 755 /logs

# 포트
EXPOSE 8080

# prod 프로필로 실행
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
