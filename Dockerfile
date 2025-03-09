# Build stage
FROM ubuntu:24.04 as build

ENV DEBIAN_FRONTEND=noninteractive

# 필요한 패키지 설치 (OpenJDK 17, Maven 등)
RUN apt-get update && \
    apt-get install -y openjdk-17-jdk maven && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY . /app
RUN mvn clean package -DskipTests

# Runtime stage
FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive

# 런타임에 필요한 OpenJDK 17 설치 (JRE 만 설치해 경량화할 수도 있음)
RUN apt-get update && \
    apt-get install -y openjdk-17-jre-headless && \
    rm -rf /var/lib/apt/lists/*

# 빌드 스테이지에서 생성된 JAR 파일 복사
RUN mkdir /app
COPY --from=build /app/target/LiveStream-0.0.1-SNAPSHOT.jar /app/LiveStream-0.0.1-SNAPSHOT.jar

# 기본 포트 (Cloud Run 등에서는 PORT 환경변수를 사용하므로 EXPOSE는 정보 제공용입니다)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/LiveStream-0.0.1-SNAPSHOT.jar"]
