# ---------- build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Dependencies first: this layer stays cached until the build files change
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q > /dev/null

COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# ---------- runtime ----------
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 appuser
WORKDIR /app
COPY --from=build /workspace/build/libs/app.jar app.jar
USER appuser

EXPOSE 8080
# 65% of container RAM for the heap leaves room for metaspace and thread stacks on small instances
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]