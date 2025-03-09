# Build stage: Gradle 기반 빌드
FROM gradle:7.6-jdk17 AS build
WORKDIR /home/gradle/project
# 프로젝트 전체 파일(빌드 스크립트, 소스 등)을 복사하고, 소유자 권한을 gradle로 설정합니다.
COPY --chown=gradle:gradle . .
# Gradle Wrapper를 사용해 빌드 (테스트는 건너뜁니다)
RUN ./gradlew clean build -x test

# Runtime stage: Ubuntu 24.04 기반 경량 이미지
FROM ubuntu:24.04
ENV DEBIAN_FRONTEND=noninteractive
# OpenJDK 17 JRE 설치
RUN apt-get update && \
    apt-get install -y openjdk-17-jre-headless && \
    rm -rf /var/lib/apt/lists/*
# 애플리케이션 파일을 저장할 디렉터리 생성
RUN mkdir /app
# 빌드 단계에서 생성된 JAR 파일 복사 (생성된 파일명이 LiveStream-0.0.1-SNAPSHOT.jar 라고 가정)
COPY --from=build /home/gradle/project/build/libs/LiveStream-0.0.1-SNAPSHOT.jar /app/LiveStream-0.0.1-SNAPSHOT.jar

# 애플리케이션이 사용하는 포트 (Cloud Run 등에서는 PORT 환경변수를 사용하지만, EXPOSE는 문서용)
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/LiveStream-0.0.1-SNAPSHOT.jar"]
