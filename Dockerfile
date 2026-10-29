# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY core/build.gradle.kts core/
COPY app/build.gradle.kts app/
RUN chmod +x gradlew && ./gradlew --no-daemon -q :app:dependencies > /dev/null

COPY core core
COPY app app
RUN ./gradlew --no-daemon :app:bootJar -x test

FROM eclipse-temurin:21-jre AS runtime
RUN groupadd --system --gid 10001 app && useradd --system --uid 10001 --gid app --no-create-home app
WORKDIR /opt/app
COPY --from=build /workspace/app/build/libs/app.jar app.jar
USER app

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 3003
ENTRYPOINT ["java", "-jar", "app.jar"]
