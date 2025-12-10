# Используем официальный образ Gradle для сборки
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Копируем файлы проекта
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY src ./src

# Собираем fat jar
RUN gradle jar --no-daemon

# Финальный образ с минимальным размером
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Копируем собранный jar из builder
COPY --from=builder /app/build/libs/*.jar app.jar

# Создаём непривилегированного пользователя
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Порт, на котором работает приложение
EXPOSE 8080

# Переменные окружения по умолчанию
ENV PORT=8080
ENV HOST=0.0.0.0
ENV OLLAMA_URL=http://ollama:11434
ENV OLLAMA_MODEL=qwen3:4b

# Запуск приложения
CMD ["java", "-jar", "app.jar"]