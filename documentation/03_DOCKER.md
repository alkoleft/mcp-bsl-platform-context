# Запуск MCP сервера в Docker

## Запуск в Docker c STDIO

- В папке с `Dockerfile'ом` рядом должен быть JAR-файл (например, `mcp-bsl-context-0.2.2.jar`).
- Собираем docker image:

```bash
docker build -t 1c-platform-mcp-server-stdio -f Dockerfile.stdio --build-arg APP_VERSION=х.х.х .
```

### Пример настройки IDE при запуске в Docker через STDIO

```json
{
    "mcpServers": {
        "1c-platform-stdio": {
            "command": "docker",
            "args": [
                "run",
                "--rm",
                "-i",
                "-v",
                "...platform/bin:/app/1c-platform:ro",
                "1c-platform-mcp-server-stdio",
                "--platform-path",
                "/app/1c-platform"
            ]
        }
    }
}
```

Если запуск будет в Windows, то путь к папке платформы должен быть без пробелов (и с экранированием слешей, если речь о конфигурации в JSON):

```json
{
    "mcpServers": {
        "1c-platform-stdio": {
            "command": "docker",
            "args": [
                "run",
                "--rm",
                "-i",
                "-v",
                "C:\\ваша_папка_без_пробелов\\bin:/app/1c-platform:ro",
                "1c-platform-mcp-server-stdio",
                "--platform-path",
                "/app/1c-platform"
            ]
        }
    }
}
```

## Запуск в Docker c проксированием в SSE

Это вариант, если хотите запускать MCP на удаленном хосте с доступом по сети.
- Для проксирования используется пакет [Supergateway](https://github.com/supercorp-ai/supergateway)
- В папке с `Dockerfile'ом` рядом должен быть JAR-файл.
- Собираем и запускаем docker:

```bash
docker build -t 1c-platform-mcp-server-sse -f Dockerfile.sse --build-arg APP_VERSION=0.2.2 .

docker run -d \
  -v ./platform:/app/1c-platform:ro \
  -p 8001:8000 \
  1c-platform-mcp-server-sse
```

### Пример настройки IDE при запуске в Docker с SSE

```json
{
    "mcpServers": {
        "1c-platform-sse": {
            "url": "http://10.0.0.x:8001/sse"
        }
}
```

## Запуск в Docker c Streamable HTTP

Вариант для HTTP-транспорта MCP в режиме `streamable` (рекомендуется для новых клиентов, включая Codex).

```bash
docker build -t 1c-platform-mcp-server-streamable -f Dockerfile.streamable --build-arg APP_VERSION=0.2.2 .

docker run -d \
  -v ./platform:/app/1c-platform:ro \
  -p 8080:8080 \
  1c-platform-mcp-server-streamable
```

### Пример настройки IDE/клиента при запуске в Docker с Streamable HTTP

```json
{
    "mcpServers": {
        "1c-platform-streamable": {
            "url": "http://10.0.0.x:8080/mcp"
        }
}
```

Актуальную версию (APP_VERSION) смотрите в [релизах](https://github.com/alkoleft/mcp-bsl-context/releases)

## Быстрый старт с опубликованными Docker-образами

Сервер MCP теперь доступен как готовый Docker-образ на GitHub Container Registry: [ghcr.io/alkoleft/mcp-bsl-context](https://github.com/alkoleft/mcp-bsl-context/pkgs/container/mcp-bsl-context)

### Получение образа

```bash
docker pull ghcr.io/alkoleft/mcp-bsl-context:v0.2.2-sse
# или для stdio:
docker pull ghcr.io/alkoleft/mcp-bsl-context:v0.2.2-stdio
```

### Запуск STDIO-версии

```bash
docker run --rm -i \
  -v /path/to/1c/platform:/app/1c-platform:ro \
  ghcr.io/alkoleft/mcp-bsl-context:v0.2.2-stdio \
  --platform-path /app/1c-platform
```

### Запуск SSE-версии (с сетевым доступом)

```bash
docker run -d \
  -v /path/to/1c/platform:/app/1c-platform:ro \
  -p 8001:8000 \
  ghcr.io/alkoleft/mcp-bsl-context:v0.2.2-sse \
  --platform-path /app/1c-platform
```

- Замените `/path/to/1c/platform` на путь к вашей установленной платформе 1С (только для чтения).
- Для актуальных тегов и digest-версий смотрите [страницу пакета](https://github.com/alkoleft/mcp-bsl-context/pkgs/container/mcp-bsl-context).

### Интеграция с IDE

Примеры конфигурации для интеграции с IDE см. в разделе [Интеграция с IDE](05_INTEGRATION.md).

## Устранение неполадок с Docker

### Проблемы с Docker
1. Убедитесь что путь к платформе 1С корректно монтируется
2. Проверьте права доступа к файлам платформы
3. Для Windows используйте пути без пробелов

### Проверка работы контейнера
```bash
# Проверка логов контейнера
docker logs <container_id>

# Проверка монтирования volumes
docker exec <container_id> ls -la /app/1c-platform

# Проверка доступности порта (для SSE)
docker port <container_id>
```
