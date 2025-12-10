# Быстрый старт на VPS

## Шаг 1: Подключение к серверу

```bash
ssh root@ваш_ip_адрес
```

## Шаг 2: Установка Docker

```bash
# Обновление системы
apt update && apt upgrade -y

# Установка Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh

# Проверка
docker --version
docker compose version
```

## Шаг 3: Настройка firewall

```bash
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw enable
```

## Шаг 4: Загрузка проекта

```bash
# Создание директории
mkdir -p /opt/ollama-chat
cd /opt/ollama-chat

# Вариант 1: Git
git clone <ваш_репозиторий> .

# Вариант 2: Загрузка с локальной машины
# На локальной машине:
# scp -r ollama-chat/* root@ваш_ip:/opt/ollama-chat/
```

## Шаг 5: Настройка окружения

```bash
cp .env.example .env
# Редактировать не обязательно, настройки по умолчанию подходят
```

## Шаг 6: Запуск

```bash
# Запуск всех сервисов
docker compose up -d --build

# Проверка статуса
docker compose ps

# Просмотр логов
docker compose logs -f
```

## Шаг 7: Загрузка модели

```bash
# Дождитесь запуска Ollama (30-60 секунд)
sleep 60

# Загрузите модель (легкая версия для экономии ресурсов)
docker exec -it ollama ollama pull qwen2.5:1.5b

# Проверьте установленные модели
docker exec -it ollama ollama list
```

## Шаг 8: Тестирование

```bash
# Проверка здоровья
curl http://localhost:8080/health

# Отправка тестового сообщения
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Привет!"}'
```

## Шаг 9: Тестирование извне

На вашем локальном компьютере:

```bash
# Замените YOUR_IP на IP вашего VPS
curl http://YOUR_IP:8080/health

# Или используйте тестовый скрипт
./test_api_server.sh http://YOUR_IP:8080
```

## Готово!

Ваш API работает на:
- **HTTP**: `http://ваш_ip:8080`
- **Nginx (если настроен)**: `http://ваш_ip`

## Полезные команды

```bash
# Просмотр логов
docker compose logs -f chat-api

# Перезапуск сервиса
docker compose restart chat-api

# Остановка всех сервисов
docker compose down

# Обновление
git pull
docker compose up -d --build
```

## Следующие шаги

1. Настройте домен и SSL (см. DEPLOYMENT.md)
2. Добавьте мониторинг
3. Настройте резервное копирование
4. Оптимизируйте под вашу нагрузку

Полная документация: [DEPLOYMENT.md](DEPLOYMENT.md)
