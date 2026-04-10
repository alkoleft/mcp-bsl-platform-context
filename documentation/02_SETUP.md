# Установка и запуск MCP сервера

## Требования

- Java 17+
- Платформа 1С Предприятие (для загрузки данных API)

## Сборка проекта

```bash
./gradlew build
```

## Запуск MCP сервера

### STDIO режим (по умолчанию)

#### Базовый запуск

```bash
java -jar build/libs/mcp-bsl-context.jar --platform-path /path/to/1c/platform
```

#### Использование переменных окружения

Вместо передачи параметра `--platform-path` можно задать переменную окружения:
- `PLATFORM_CONTEXT_PATH` — путь к каталогу установки 1С Предприятия (аналогично `--platform-path`)

Для управления логированием можно задать переменную окружения:
- `LOG_FILE` — путь к файлу для логов сервера (по умолчанию `mcp-server.log` в рабочей директории)

Пример запуска с переменными окружения:

```bash
PLATFORM_CONTEXT_PATH="/opt/1cv8/x86_64/8.3.25.1257" LOG_FILE="/tmp/mcp-server.log" \
  java -jar build/libs/mcp-bsl-context.jar
```

### SSE режим (Server-Sent Events)

#### Базовый запуск

```bash
java -jar build/libs/mcp-bsl-context.jar --mode sse --platform-path /path/to/1c/platform
```

#### С кастомным портом

```bash
java -jar build/libs/mcp-bsl-context.jar --mode sse --port 9000 --platform-path /path/to/1c/platform
```

#### Переменные окружения для SSE

```bash
# Настройка через переменные окружения
export PLATFORM_CONTEXT_PATH="/path/to/1c/platform"
export SSE_PORT=8080
java -jar build/libs/mcp-bsl-context.jar --mode sse
```

### Параметры запуска

- `--platform-path` - путь к каталогу установки 1С Предприятия
- `--mode` - режим работы (`stdio`, `sse` или `streamable`, по умолчанию `stdio`)
- `--port` - порт для SSE режима (по умолчанию 8080)
- `--verbose` - включить отладочное логирование
- `--help` - показать справку

### Пример запуска с отладкой

```bash
# STDIO режим
java -jar build/libs/mcp-bsl-context.jar \
  --platform-path "/opt/1cv8/x86_64/8.3.25.1257" \
  --verbose

# SSE режим
java -jar build/libs/mcp-bsl-context.jar \
  --mode sse \
  --platform-path "/opt/1cv8/x86_64/8.3.25.1257" \
  --port 9000 \
  --verbose
```

## Логирование

- **Рабочие логи** - `mcp-server.log` (автоматическая ротация)
- **Отладочные сообщения** - stderr (не мешает MCP протоколу)
- **JSON-RPC коммуникация** - через stdout/stdin (STDIO режим)
- **HTTP логи** - в консоли и файле (SSE режим)

## Устранение неполадок

### Сервер не запускается
1. Проверьте корректность пути к платформе 1С
2. Убедитесь что Java 17+ установлена
3. Проверьте логи в `mcp-server.log`
4. В SSE режиме проверьте доступность порта

### MCP клиент не видит tools
1. Убедитесь что сервер полностью загрузился
2. Проверьте совместимость версий MCP протокола
3. Включите отладочное логирование (`--verbose`)

### SSE соединение не устанавливается
1. Проверьте что сервер запущен в SSE режиме (`--mode sse`)
2. Убедитесь что порт не занят другими приложениями
3. Проверьте сетевые настройки и firewall

### Медленный поиск
1. Поиск индексируется при запуске - дождитесь завершения
2. При большом объеме данных увеличьте heap size JVM
3. Результаты кэшируются автоматически

## Техническая поддержка

- **Версия MCP протокола** - 2024-11-05
- **Версия Spring AI** - 1.0.0
- **Минимальная версия Java** - 17
- **Поддерживаемые ОС** - Linux, macOS, Windows
- **Поддерживаемые транспорты** - STDIO, HTTP/SSE

### Полезные ссылки
- [Репозиторий проекта](https://github.com/alkoleft/mcp-bsl-context)
- [GitHub Container Registry](https://github.com/alkoleft/mcp-bsl-context/pkgs/container/mcp-bsl-context)
- [Релизы](https://github.com/alkoleft/mcp-bsl-context/releases)
- [SSE документация](SSE_USAGE.md)

Для сообщения об ошибках создайте issue в репозитории проекта.
Для сообщения об ошибках создайте issue в репозитории проекта.
