# Multi-stage build para minimizar tamanho da imagem
FROM maven:3.9-eclipse-temurin-25-alpine AS build

WORKDIR /app

# Copiar apenas pom.xml primeiro para cache de dependências
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fonte e compilar
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage final - runtime
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Criar usuário não-root para segurança
RUN addgroup -g 1001 pitstop && \
    adduser -D -u 1001 -G pitstop pitstop

# Criar diretório de uploads (será sobrescrito pelo volume, mas garante que existe)
RUN mkdir -p /var/pitstop/uploads && \
    chown -R pitstop:pitstop /var/pitstop

# Copiar JAR do stage de build
COPY --from=build /app/target/*.jar app.jar

# Ajustar permissões
RUN chown -R pitstop:pitstop /app

USER pitstop

# Expor porta
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Configurar JVM para container
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# Executar aplicação
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
