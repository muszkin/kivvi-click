# syntax=docker/dockerfile:1

# Stage 1: build the Vue 3 SPA. Kept separate from the Maven stage so the packaging stage
# never needs Node on its image — it only copies the built static files.
FROM node:26-alpine AS frontend-build
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2: package the Spring Boot jar with the SPA dist copied into the resources the jar
# serves from, so a single artifact carries both.
FROM maven:3.9-eclipse-temurin-25 AS backend-build
WORKDIR /backend
COPY backend/pom.xml backend/mvnw ./
COPY backend/.mvn/ .mvn/
COPY backend/src/ src/
COPY --from=frontend-build /frontend/dist/. src/main/resources/static/
RUN mvn -q -DskipTests package

# Stage 3: run the jar as a non-root user on a bare JRE.
FROM eclipse-temurin:25-jre AS runtime
# curl: the only thing the base image is missing for compose's own health check
# (GET /actuator/health) — everything else about this image is otherwise minimal on purpose.
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
RUN groupadd --system kivvi && useradd --system --gid kivvi --home-dir /app kivvi
WORKDIR /app
COPY --from=backend-build /backend/target/kivvi-click-*.jar app.jar
# Created here rather than left to the application, because compose bind-mounts ./var/mail into
# this tree (PIO-71's dev maildrop). Docker creates any missing parent of a mount point as
# root-owned, and a root-owned /app/var stops the unprivileged runtime user from ever creating
# /app/var/import — which turns every customer-import upload into an AccessDeniedException.
RUN mkdir -p /app/var/import /app/var/mail
RUN chown -R kivvi:kivvi /app
USER kivvi
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
