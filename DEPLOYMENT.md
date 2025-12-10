# Инструкция по развёртыванию Ollama Chat API на VPS

## Содержание
1. [Требования](#требования)
2. [Подготовка VPS](#подготовка-vps)
3. [Установка Docker и Docker Compose](#установка-docker-и-docker-compose)
4. [Развёртывание приложения](#развёртывание-приложения)
5. [Настройка Ollama и загрузка модели](#настройка-ollama-и-загрузка-модели)
6. [Тестирование API](#тестирование-api)
7. [Настройка SSL (HTTPS)](#настройка-ssl-https)
8. [Устранение неполадок](#устранение-неполадок)

---

## Требования

### Минимальные требования к VPS:
- **CPU**: 2+ ядра (рекомендуется 4)
- **RAM**: 4 GB (минимум для модели qwen2.5:1.5b)
- **Диск**: 10 GB свободного места
- **ОС**: Ubuntu 20.04/22.04 или Debian 11/12
- **GPU**: Опционально (для ускорения, требуется NVIDIA с CUDA)

### Рекомендуемые конфигурации:

| Конфигурация | CPU | RAM | Диск | Модель | Цена (примерно) |
|--------------|-----|-----|------|--------|-----------------|
| **Бюджет** | 2 ядра | 4 GB | 10 GB | qwen2.5:1.5b | €5-10/мес |
| **Оптимум** | 4 ядра | 6-8 GB | 20 GB | qwen2.5:3b | €10-20/мес |
| **Мощный** | 4+ ядра | 12+ GB | 30 GB | qwen3:4b+ | €30+/мес |

---

## Подготовка VPS

### 1. Подключитесь к серверу:
```bash
ssh root@ваш_ip_адрес
```

### 2. Обновите систему:
```bash
apt update && apt upgrade -y
```

### 3. Установите необходимые пакеты:
```bash
apt install -y curl git vim ufw
```

### 4. Настройте firewall:
```bash
# Разрешаем SSH
ufw allow 22/tcp

# Разрешаем HTTP и HTTPS
ufw allow 80/tcp
ufw allow 443/tcp

# Опционально: разрешаем прямой доступ к API
ufw allow 8080/tcp

# Включаем firewall
ufw enable
```

---

## Установка Docker и Docker Compose

### 1. Установите Docker:
```bash
# Удаляем старые версии (если есть)
apt remove docker docker-engine docker.io containerd runc

# Устанавливаем зависимости
apt install -y ca-certificates curl gnupg lsb-release

# Добавляем GPG ключ Docker
mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# Добавляем репозиторий Docker
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null

# Устанавливаем Docker
apt update
apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Проверяем установку
docker --version
docker compose version
```

### 2. Настройте Docker:
```bash
# Добавляем Docker в автозагрузку
systemctl enable docker
systemctl start docker

# Проверяем статус
systemctl status docker
```

---

## Развёртывание приложения

### 1. Создайте директорию для проекта:
```bash
mkdir -p /opt/ollama-chat
cd /opt/ollama-chat
```

### 2. Клонируйте проект (или загрузите файлы):
```bash
# Вариант 1: Если проект в Git
git clone <ваш_репозиторий> .

# Вариант 2: Загрузите файлы вручную через scp
# На локальной машине выполните:
# scp -r /путь/к/ollama-chat/* root@ваш_ip:/opt/ollama-chat/
```

### 3. Создайте .env файл:
```bash
cp .env.example .env
vim .env
```

Отредактируйте переменные окружения по необходимости:
```env
OLLAMA_MODEL=qwen3:4b
OLLAMA_URL=http://ollama:11434
PORT=8080
HOST=0.0.0.0
```

### 4. Запустите контейнеры:
```bash
# Сборка и запуск всех сервисов
docker compose up -d --build

# Проверка статуса
docker compose ps

# Просмотр логов
docker compose logs -f
```

---

## Настройка Ollama и загрузка модели

### 1. Подождите, пока Ollama запустится (30-60 секунд):
```bash
docker compose logs ollama
```

### 2. Загрузите модель:
```bash
# Войдите в контейнер Ollama
docker exec -it ollama bash

# Загрузите модель (это займёт время, ~900 MB для qwen2.5:1.5b)
ollama pull qwen2.5:1.5b

# Проверьте установленные модели
ollama list

# Выйдите из контейнера
exit
```

### 3. Список популярных моделей:

| Модель | Размер | RAM | Загрузка | Качество | Скорость |
|--------|--------|-----|----------|----------|----------|
| `qwen2.5:1.5b` ⭐ | 1.5B | 4 GB | ~900 MB | ⭐⭐⭐ | ⚡⚡⚡⚡⚡ |
| `qwen2.5:3b` | 3B | 6 GB | ~2 GB | ⭐⭐⭐⭐ | ⚡⚡⚡⚡ |
| `phi3:3.8b` | 3.8B | 6 GB | ~2.3 GB | ⭐⭐⭐⭐ | ⚡⚡⚡⚡ |
| `gemma:2b` | 2B | 4 GB | ~1.5 GB | ⭐⭐⭐ | ⚡⚡⚡⚡⚡ |
| `qwen3:4b` | 4B | 8 GB | ~2.5 GB | ⭐⭐⭐⭐ | ⚡⚡⚡⚡ |
| `llama2:7b` | 7B | 10 GB | ~4 GB | ⭐⭐⭐⭐⭐ | ⚡⚡⚡ |
| `mistral:7b` | 7B | 10 GB | ~4 GB | ⭐⭐⭐⭐⭐ | ⚡⚡⚡ |

**Рекомендация**: Для бюджетных VPS (4-6 GB RAM) используйте `qwen2.5:1.5b` или `qwen2.5:3b`

---

## Тестирование API

### 1. Проверка health endpoint:
```bash
curl http://localhost:8080/health
```

Ожидаемый ответ:
```json
{
  "status": "healthy",
  "ollamaConnected": true,
  "timestamp": 1234567890
}
```

### 2. Проверка информации о сервисе:
```bash
curl http://localhost:8080/api/info
```

### 3. Отправка сообщения:
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Привет! Как дела?",
    "model": "qwen2.5:1.5b"
  }'
```

### 4. Тестирование с внешнего клиента:
```bash
# Замените YOUR_SERVER_IP на IP вашего сервера
curl -X POST http://YOUR_SERVER_IP:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Hello!",
    "model": "qwen2.5:1.5b"
  }'
```

### 5. Использование сессий (для сохранения контекста):
```bash
# Отправляем первое сообщение с идентификатором сессии
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -H "X-Session-ID: user123" \
  -d '{"message": "Меня зовут Иван"}'

# Второе сообщение в той же сессии
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -H "X-Session-ID: user123" \
  -d '{"message": "Как меня зовут?"}'

# Очистка истории сессии
curl -X POST http://localhost:8080/api/clear \
  -H "X-Session-ID: user123"
```

---

## Настройка SSL (HTTPS)

### Вариант 1: С использованием Certbot (Let's Encrypt)

#### 1. Установите Certbot:
```bash
apt install -y certbot
```

#### 2. Получите SSL сертификат:
```bash
# Остановите nginx временно
docker compose stop nginx

# Получите сертификат
certbot certonly --standalone -d your-domain.com

# Сертификаты будут сохранены в:
# /etc/letsencrypt/live/your-domain.com/
```

#### 3. Скопируйте сертификаты:
```bash
mkdir -p /opt/ollama-chat/ssl
cp /etc/letsencrypt/live/your-domain.com/fullchain.pem /opt/ollama-chat/ssl/
cp /etc/letsencrypt/live/your-domain.com/privkey.pem /opt/ollama-chat/ssl/
```

#### 4. Отредактируйте nginx.conf:
Раскомментируйте секцию HTTPS сервера и настройте под ваш домен.

#### 5. Перезапустите nginx:
```bash
docker compose up -d nginx
```

#### 6. Настройте автоматическое обновление сертификатов:
```bash
# Создайте скрипт обновления
cat > /opt/ollama-chat/renew-cert.sh << 'EOF'
#!/bin/bash
docker compose stop nginx
certbot renew
cp /etc/letsencrypt/live/your-domain.com/fullchain.pem /opt/ollama-chat/ssl/
cp /etc/letsencrypt/live/your-domain.com/privkey.pem /opt/ollama-chat/ssl/
docker compose start nginx
EOF

chmod +x /opt/ollama-chat/renew-cert.sh

# Добавьте в crontab (запуск каждую неделю)
(crontab -l 2>/dev/null; echo "0 0 * * 0 /opt/ollama-chat/renew-cert.sh") | crontab -
```

### Вариант 2: Без собственного домена (самоподписанный сертификат)

```bash
# Создайте самоподписанный сертификат
mkdir -p /opt/ollama-chat/ssl
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout /opt/ollama-chat/ssl/privkey.pem \
  -out /opt/ollama-chat/ssl/fullchain.pem \
  -subj "/C=RU/ST=Moscow/L=Moscow/O=Company/CN=localhost"
```

---

## Управление сервисами

### Основные команды Docker Compose:

```bash
# Просмотр статуса
docker compose ps

# Просмотр логов
docker compose logs -f

# Только логи API
docker compose logs -f chat-api

# Перезапуск всех сервисов
docker compose restart

# Перезапуск конкретного сервиса
docker compose restart chat-api

# Остановка всех сервисов
docker compose down

# Остановка с удалением volumes (ОСТОРОЖНО: удалит данные Ollama!)
docker compose down -v

# Обновление и пересборка
docker compose up -d --build
```

### Мониторинг ресурсов:

```bash
# Использование ресурсов контейнерами
docker stats

# Дисковое пространство
df -h

# Свободная память
free -h
```

---

## Устранение неполадок

### Проблема: Ollama не подключается

**Решение:**
```bash
# Проверьте статус Ollama
docker compose logs ollama

# Проверьте, доступен ли Ollama внутри сети
docker exec chat-api curl http://ollama:11434/api/tags

# Перезапустите Ollama
docker compose restart ollama
```

### Проблема: Недостаточно памяти

**Решение:**
```bash
# Проверьте использование памяти
free -h

# Используйте меньшую модель
# В .env измените на более лёгкую модель:
# OLLAMA_MODEL=qwen2.5:1.5b  (требует ~4 GB)
# или
# OLLAMA_MODEL=gemma:2b      (требует ~4 GB)

vim .env
docker compose restart

# Загрузите новую модель
docker exec -it ollama ollama pull qwen2.5:1.5b
```

### Проблема: Медленная работа модели

**Решение:**
1. Используйте VPS с GPU (NVIDIA) - ускорение в 5-10 раз
2. Выберите меньшую модель (qwen2.5:1.5b быстрее чем 3b/4b)
3. Увеличьте RAM и CPU на сервере
4. Используйте SSD диск вместо HDD

### Проблема: Порты заняты

**Решение:**
```bash
# Проверьте, какой процесс использует порт
netstat -tulpn | grep 8080

# Измените порты в docker-compose.yml
vim docker-compose.yml
```

### Проблема: Docker Compose не найден

**Решение:**
```bash
# Установите docker-compose-plugin
apt install -y docker-compose-plugin

# Или используйте старый синтаксис
docker-compose up -d
```

---

## Backup и восстановление

### Backup данных Ollama:

```bash
# Создайте backup
docker run --rm -v ollama-chat_ollama_data:/data -v /opt/backups:/backup ubuntu tar czf /backup/ollama-backup-$(date +%Y%m%d).tar.gz -C /data .
```

### Восстановление:

```bash
# Восстановите данные
docker run --rm -v ollama-chat_ollama_data:/data -v /opt/backups:/backup ubuntu tar xzf /backup/ollama-backup-YYYYMMDD.tar.gz -C /data
```

---

## API Документация

### Endpoints:

1. **GET /** - Главная страница с описанием API

2. **GET /health** - Проверка здоровья сервиса
   - Response: `{ "status": "healthy", "ollamaConnected": true }`

3. **POST /api/chat** - Отправка сообщения
   - Headers:
     - `Content-Type: application/json`
     - `X-Session-ID: <session-id>` (опционально)
   - Body: `{ "message": "текст", "model": "qwen3:4b" }`
   - Response: `{ "response": "ответ", "model": "qwen3:4b" }`

4. **POST /api/clear** - Очистка истории чата
   - Headers: `X-Session-ID: <session-id>` (опционально)
   - Response: `{ "message": "История чата очищена" }`

5. **GET /api/info** - Информация о сервисе
   - Response: `{ "service": "...", "version": "...", ... }`

---

## Производственные рекомендации

1. **Безопасность:**
   - Настройте SSL/TLS (HTTPS)
   - Используйте firewall
   - Ограничьте доступ по IP (если нужно)
   - Добавьте аутентификацию (JWT токены)

2. **Мониторинг:**
   - Настройте логирование
   - Используйте системы мониторинга (Prometheus, Grafana)
   - Настройте алерты

3. **Масштабирование:**
   - Используйте балансировщик нагрузки
   - Разверните несколько инстансов chat-api
   - Используйте Redis для сессий

4. **Резервное копирование:**
   - Регулярный backup данных Ollama
   - Backup конфигурационных файлов
   - Версионный контроль кода

---

## Полезные ссылки

- [Ollama Documentation](https://github.com/ollama/ollama)
- [Ktor Documentation](https://ktor.io/docs/)
- [Docker Documentation](https://docs.docker.com/)
- [Let's Encrypt](https://letsencrypt.org/)

---

## Поддержка

Если возникли проблемы:
1. Проверьте логи: `docker compose logs -f`
2. Проверьте статус сервисов: `docker compose ps`
3. Проверьте документацию Ollama
4. Создайте issue в репозитории проекта
