#!/bin/bash

# Скрипт для автоматического развёртывания Ollama Chat API на VPS

set -e  # Остановка при ошибке

# Цвета для вывода
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Развёртывание Ollama Chat API${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Проверка, что скрипт запущен с правами root
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}Пожалуйста, запустите скрипт с правами root (sudo)${NC}"
    exit 1
fi

# Проверка наличия Docker
if ! command -v docker &> /dev/null; then
    echo -e "${YELLOW}[1/7] Установка Docker...${NC}"

    # Установка зависимостей
    apt update
    apt install -y ca-certificates curl gnupg lsb-release

    # Добавление GPG ключа
    mkdir -p /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg

    # Добавление репозитория
    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
      $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null

    # Установка Docker
    apt update
    apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

    # Запуск Docker
    systemctl enable docker
    systemctl start docker

    echo -e "${GREEN}Docker установлен успешно!${NC}"
else
    echo -e "${GREEN}[1/7] Docker уже установлен${NC}"
fi

# Проверка наличия .env файла
echo -e "${YELLOW}[2/7] Проверка конфигурации...${NC}"
if [ ! -f .env ]; then
    echo -e "${YELLOW}.env файл не найден. Создаём из .env.example...${NC}"
    cp .env.example .env
    echo -e "${GREEN}.env файл создан${NC}"
else
    echo -e "${GREEN}.env файл уже существует${NC}"
fi

# Настройка firewall (если установлен ufw)
if command -v ufw &> /dev/null; then
    echo -e "${YELLOW}[3/7] Настройка firewall...${NC}"
    ufw allow 22/tcp   # SSH
    ufw allow 80/tcp   # HTTP
    ufw allow 443/tcp  # HTTPS
    ufw allow 8080/tcp # API (опционально)
    echo "y" | ufw enable || true
    echo -e "${GREEN}Firewall настроен${NC}"
else
    echo -e "${YELLOW}[3/7] UFW не установлен, пропускаем настройку firewall${NC}"
fi

# Остановка существующих контейнеров
echo -e "${YELLOW}[4/7] Остановка существующих контейнеров...${NC}"
docker compose down || true

# Сборка и запуск контейнеров
echo -e "${YELLOW}[5/7] Сборка и запуск контейнеров...${NC}"
docker compose up -d --build

# Ожидание запуска Ollama
echo -e "${YELLOW}[6/7] Ожидание запуска Ollama (60 секунд)...${NC}"
sleep 60

# Загрузка модели
echo -e "${YELLOW}[7/7] Загрузка модели Ollama...${NC}"
OLLAMA_MODEL=$(grep OLLAMA_MODEL .env | cut -d '=' -f2 | tr -d '"' | tr -d "'" || echo "qwen2.5:1.5b")
echo -e "${BLUE}Загружается модель: $OLLAMA_MODEL${NC}"
docker exec ollama ollama pull $OLLAMA_MODEL

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Развёртывание завершено!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Получение IP адреса
IP_ADDR=$(hostname -I | awk '{print $1}')

echo -e "${BLUE}Сервис доступен по адресу:${NC}"
echo -e "  - API: ${GREEN}http://$IP_ADDR:8080${NC}"
echo -e "  - Nginx: ${GREEN}http://$IP_ADDR${NC}"
echo ""

echo -e "${BLUE}Проверка состояния:${NC}"
docker compose ps

echo ""
echo -e "${BLUE}Тестирование API:${NC}"
sleep 5
curl -s http://localhost:8080/health | python3 -m json.tool 2>/dev/null || curl -s http://localhost:8080/health

echo ""
echo -e "${YELLOW}Для просмотра логов:${NC} docker compose logs -f"
echo -e "${YELLOW}Для перезапуска:${NC} docker compose restart"
echo -e "${YELLOW}Для остановки:${NC} docker compose down"
echo ""
