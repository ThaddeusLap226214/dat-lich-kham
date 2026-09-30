# syntax=docker/dockerfile:1
# Một Dockerfile dùng chung cho mọi module; docker-compose truyền tên module qua MODULE
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY . .
ARG MODULE
# sharing=locked: các service build lần lượt trên cùng kho thư viện ~/.m2, tránh ghi đè lẫn nhau
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -q -pl ${MODULE} -am package -DskipTests \
    && cp ${MODULE}/target/${MODULE}-*.jar /workspace/app.jar

FROM eclipse-temurin:25-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
ENV TZ=Asia/Ho_Chi_Minh
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
# Tiết kiệm RAM khi chạy 7 service trên một máy: bộ dọn rác đơn giản, JIT mức 1, stack luồng nhỏ
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-Xss512k", "-jar", "app.jar"]
