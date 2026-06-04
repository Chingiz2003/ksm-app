# Инструкция для проверки проекта

## Что нужно установить

1. Docker Desktop.
2. Запустить Docker Desktop и дождаться статуса `running`.

Проверка установки:

```bash
docker --version
docker compose version
```

## Как запустить проект

В терминале перейти в папку проекта и выполнить:

```bash
docker compose up --build
```

После запуска открыть:

```text
http://localhost:5173
```

## Тестовые аккаунты

| Роль | Логин | Пароль |
| --- | --- | --- |
| Закупщик | `buyer` | `buyer123` |
| Менеджер продаж | `sales` | `sales123` |
| Руководитель | `manager` | `manager123` |

## Как остановить

```bash
docker compose down
```

Если нужно очистить БД и заново загрузить seed-данные из Excel:

```bash
docker compose down -v
docker compose up --build
```
