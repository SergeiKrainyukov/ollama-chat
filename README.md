# Ollama Chat API

REST API сервер для работы с Ollama LLM моделями на базе Ktor.

## Возможности

- REST API для общения с LLM моделями через Ollama
- Поддержка контекста разговора (сессии)
- Готовая Docker конфигурация
- Nginx reverse proxy
- Поддержка различных моделей
- Оптимизирован для работы на бюджетных VPS (от 4 GB RAM)

## Быстрый старт (локально)

```bash
# 1. Запустите все сервисы
docker compose up -d --build

# 2. Дождитесь запуска Ollama и загрузите модель
docker exec -it ollama ollama pull qwen2.5:1.5b

# 3. Протестируйте API
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Привет!"}'
```

## API Endpoints

- `GET /` - Главная страница с описанием
- `GET /health` - Проверка здоровья сервиса
- `POST /api/chat` - Отправить сообщение
- `POST /api/clear` - Очистить историю чата
- `GET /api/info` - Информация о сервисе

## Развёртывание на VPS

Подробная инструкция по развёртыванию находится в файле [DEPLOYMENT.md](DEPLOYMENT.md).

### Кратко:

1. Установите Docker и Docker Compose на VPS
2. Склонируйте проект на сервер
3. Создайте `.env` файл на основе `.env.example`
4. Запустите: `docker compose up -d --build`
5. Загрузите модель: `docker exec -it ollama ollama pull qwen2.5:1.5b`

## Конфигурация

Переменные окружения в `.env`:

```env
OLLAMA_MODEL=qwen2.5:1.5b        # Модель Ollama (легкая, требует ~4 GB RAM)
OLLAMA_URL=http://ollama:11434   # URL Ollama API
PORT=8080                        # Порт API сервера
HOST=0.0.0.0                     # Хост API сервера
```

## Примеры использования

### Простой запрос:
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Объясни квантовую механику простыми словами"
  }'
```

### С сохранением контекста (сессии):
```bash
# Первое сообщение
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -H "X-Session-ID: user123" \
  -d '{"message": "Меня зовут Иван"}'

# Второе сообщение в той же сессии
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -H "X-Session-ID: user123" \
  -d '{"message": "Как меня зовут?"}'
```

### Очистка истории:
```bash
curl -X POST http://localhost:8080/api/clear \
  -H "X-Session-ID: user123"
```

## Требования

- Docker и Docker Compose
- **4+ GB RAM** (для модели qwen2.5:1.5b)
- 10+ GB свободного места на диске

**Рекомендуется**: 6-8 GB RAM для комфортной работы

## Технологии

- **Kotlin** - язык программирования
- **Ktor** - веб-фреймворк
- **Ollama** - платформа для запуска LLM моделей
- **Docker** - контейнеризация
- **Nginx** - reverse proxy

## Поддерживаемые модели

По умолчанию используется `qwen2.5:1.5b` - оптимальна для бюджетных VPS:

| Модель | Размер | RAM | Качество | Скорость |
|--------|--------|-----|----------|----------|
| `qwen2.5:1.5b` ⭐ | 1.5B | 4 GB | ⭐⭐⭐ | ⚡⚡⚡⚡⚡ |
| `qwen2.5:3b` | 3B | 6 GB | ⭐⭐⭐⭐ | ⚡⚡⚡⚡ |
| `phi3:3.8b` | 3.8B | 6 GB | ⭐⭐⭐⭐ | ⚡⚡⚡⚡ |
| `gemma:2b` | 2B | 4 GB | ⭐⭐⭐ | ⚡⚡⚡⚡⚡ |
| `qwen3:4b` | 4B | 8 GB | ⭐⭐⭐⭐ | ⚡⚡⚡⚡ |
| `llama2:7b` | 7B | 10 GB | ⭐⭐⭐⭐⭐ | ⚡⚡⚡ |
| `mistral:7b` | 7B | 10 GB | ⭐⭐⭐⭐⭐ | ⚡⚡⚡ |

Для смены модели измените `OLLAMA_MODEL` в `.env` файле.

Полный список: https://ollama.com/library

## Устранение неполадок

Смотрите раздел "Устранение неполадок" в [DEPLOYMENT.md](DEPLOYMENT.md).

## Лицензия

MIT
