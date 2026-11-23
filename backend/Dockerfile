FROM gradle:8.13-jdk21-alpine AS builder
WORKDIR /build
COPY settings.gradle.kts build.gradle.kts ./
COPY src ./src
RUN gradle installDist --no-daemon

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S telefam && adduser -S telefam -G telefam
WORKDIR /app
COPY --from=builder /build/build/install/telefam-server /app
RUN mkdir -p /var/lib/telefam/avatars && chown -R telefam:telefam /var/lib/telefam /app
USER telefam
EXPOSE 8080
ENTRYPOINT ["/app/bin/telefam-server"]
