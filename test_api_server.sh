#!/bin/bash

# Цвета для вывода
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Базовый URL (измените на IP вашего сервера)
BASE_URL="${1:-http://localhost:8080}"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Тестирование Ollama Chat API${NC}"
echo -e "${BLUE}  URL: $BASE_URL${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Тест 1: Главная страница
echo -e "${GREEN}[1] GET /${NC}"
curl -s "$BASE_URL/"
echo -e "\n"

# Тест 2: Health check
echo -e "${GREEN}[2] GET /health${NC}"
curl -s "$BASE_URL/health" | python3 -m json.tool 2>/dev/null || curl -s "$BASE_URL/health"
echo -e "\n"

# Тест 3: Информация о сервисе
echo -e "${GREEN}[3] GET /api/info${NC}"
curl -s "$BASE_URL/api/info" | python3 -m json.tool 2>/dev/null || curl -s "$BASE_URL/api/info"
echo -e "\n"

# Тест 4: Отправка простого сообщения
echo -e "${GREEN}[4] POST /api/chat - Простое сообщение${NC}"
curl -s -X POST "$BASE_URL/api/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Привет! Представься, пожалуйста."
  }' | python3 -m json.tool 2>/dev/null || curl -s -X POST "$BASE_URL/api/chat" \
  -H "Content-Type: application/json" \
  -d '{"message": "Привет! Представься, пожалуйста."}'
echo -e "\n"

# Тест 5: Сообщения с контекстом (сессия)
echo -e "${GREEN}[5] POST /api/chat - Первое сообщение с сессией${NC}"
curl -s -X POST "$BASE_URL/api/chat" \
  -H "Content-Type: application/json" \
  -H "X-Session-ID: test-session-123" \
  -d '{
    "message": "Меня зовут Сергей. Запомни это."
  }' | python3 -m json.tool 2>/dev/null || curl -s -X POST "$BASE_URL/api/chat" \
  -H "Content-Type: application/json" \
  -H "X-Session-ID: test-session-123" \
  -d '{"message": "Меня зовут Сергей. Запомни это."}'
echo -e "\n"

echo -e "${GREEN}[6] POST /api/chat - Второе сообщение в той же сессии${NC}"
curl -s -X POST "$BASE_URL/api/chat" \
  -H "Content-Type: application/json" \
  -H "X-Session-ID: test-session-123" \
  -d '{
    "message": "Как меня зовут?"
  }' | python3 -m json.tool 2>/dev/null || curl -s -X POST "$BASE_URL/api/chat" \
  -H "Content-Type: application/json" \
  -H "X-Session-ID: test-session-123" \
  -d '{"message": "Как меня зовут?"}'
echo -e "\n"

# Тест 7: Очистка истории
echo -e "${GREEN}[7] POST /api/clear - Очистка истории сессии${NC}"
curl -s -X POST "$BASE_URL/api/clear" \
  -H "X-Session-ID: test-session-123" | python3 -m json.tool 2>/dev/null || curl -s -X POST "$BASE_URL/api/clear" \
  -H "X-Session-ID: test-session-123"
echo -e "\n"

# Тест 8: Проверка, что контекст очищен
echo -e "${GREEN}[8] POST /api/chat - Проверка очистки контекста${NC}"
curl -s -X POST "$BASE_URL/api/chat" \
  -H "Content-Type: application/json" \
  -H "X-Session-ID: test-session-123" \
  -d '{
    "message": "Как меня зовут?"
  }' | python3 -m json.tool 2>/dev/null || curl -s -X POST "$BASE_URL/api/chat" \
  -H "Content-Type: application/json" \
  -H "X-Session-ID: test-session-123" \
  -d '{"message": "Как меня зовут?"}'
echo -e "\n"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Тестирование завершено!${NC}"
echo -e "${BLUE}========================================${NC}"
