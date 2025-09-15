# 1) Builder stage
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn -q -e -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -e -B -DskipTests package

# 2) Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Etc/UTC
ENV JAVA_OPTS=""
# App works in 10000
EXPOSE ${PORT:-10000}
# Copy Jar
COPY --from=builder /build/target/todo-app.jar /app/app.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]