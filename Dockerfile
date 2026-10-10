# Imagens base fixadas por digest (atualize com o Dependabot ou conferindo o digest no Docker Hub).
FROM maven:3.9.9-eclipse-temurin-11@sha256:8d3b35643e52d707b16a3e9b52698be1b75c2b45beb5d0e37d35e881f0a18ced AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Os testes rodam no CI (pipeline-dev.yml); a imagem só empacota.
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:25-jre-jammy@sha256:25777acfabf927084b7ef46d8bc786b6203c8c344f56541054238b8c4fe73db9
RUN groupadd --system --gid 10001 nord && useradd --system --uid 10001 --gid nord --no-create-home nord
WORKDIR /app
COPY --from=build --chown=nord:nord /build/target/*.jar app.jar
USER 10001

# Heap proporcional ao limite de memória do container; encerra em OutOfMemory em vez de seguir degradado.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -Djava.io.tmpdir=/tmp"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
