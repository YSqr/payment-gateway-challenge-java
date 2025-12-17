FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

COPY . .

RUN chmod +x gradlew

RUN ./gradlew bootJar --no-daemon

FROM build
WORKDIR /app

RUN addgroup --system checkout && adduser --system --ingroup checkout checkout
USER checkout:checkout

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]