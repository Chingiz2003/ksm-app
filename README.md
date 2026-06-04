# KSM Fruit

Веб-приложение для ведения поставщиков, заказов клиентов и финансовой сводки по тестовому заданию.

## Стек

- Backend: Spring Boot 3, Spring Security, Spring Data JPA
- Frontend: React + Vite
- БД: PostgreSQL
- Seed-данные: `data/Таблица 1.xlsx`, `data/Таблица 2.xlsx`

## Тестовые аккаунты

| Роль | Логин | Пароль | Права |
| --- | --- | --- | --- |
| Закупщик | `buyer` | `buyer123` | CRUD поставщиков, просмотр заказов |
| Менеджер продаж | `sales` | `sales123` | CRUD заказов, просмотр поставщиков, свои финансы |
| Руководитель | `manager` | `manager123` | Полный доступ ко всем разделам |

## Запуск локально без Docker

### Быстрая проверка без PostgreSQL

Если нужно просто проверить веб-приложение, можно запустить backend с временной H2 БД:

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=local-h2'
```

В этом режиме данные из Excel загрузятся при каждом запуске, но после остановки backend изменения пропадут.

### Запуск с локальным PostgreSQL

1. Подготовить локальную PostgreSQL БД:

```bash
./scripts/setup-local-postgres.sh
```

Скрипт создаст или обновит:

- БД: `ksm_fruit`
- пользователя: `ksm`
- пароль: `ksm`

Если ваш PostgreSQL требует вход под отдельным админ-пользователем, используйте переменные окружения/параметры `psql`, например:

```bash
ADMIN_DB=postgres ./scripts/setup-local-postgres.sh
```

2. Запустить backend:

```bash
cd backend
./gradlew bootRun
```

Backend будет доступен на `http://localhost:8080`.

3. Запустить frontend:

```bash
cd frontend
npm install
npm run dev
```

Frontend будет доступен на `http://localhost:5173`.

## Запуск через Docker

Docker-запуск поднимает сразу три контейнера:

- `postgres` — база данных PostgreSQL
- `backend` — Spring Boot API
- `frontend` — React-приложение через nginx

### Что установить

На macOS проще всего установить Docker Desktop:

1. Скачать Docker Desktop с официальной страницы: https://docs.docker.com/installation/mac/
2. Установить и запустить Docker Desktop.
3. Дождаться статуса `Docker Desktop is running`.
4. Проверить в терминале:

```bash
docker --version
docker compose version
```

### Как запустить проект

Из корня проекта:

```bash
docker compose up --build
```

После запуска:

- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080`
- PostgreSQL доступен только внутри Docker-сети как `postgres:5432`

### Как остановить

```bash
docker compose down
```

Если нужно удалить данные PostgreSQL и начать seed заново:

```bash
docker compose down -v
```

## Как работает алгоритм

При создании или изменении заказа backend автоматически:

1. Ищет поставщиков нужного фрукта.
2. Оставляет поставщиков, у которых хватает остатка на складе.
3. Проверяет, что срок поставки поставщика не больше разницы между датой заказа и датой поставки клиенту.
4. Выбирает поставщика с минимальной ценой закупки.
5. Рассчитывает:
   - `Цена продажи = Цена закупки * 1.25`
   - `Сумма заказа = Количество * Цена продажи`
   - `Себестоимость = Количество * Цена закупки`
   - `Прибыль = Сумма заказа - Себестоимость`
6. Уменьшает остаток выбранного поставщика.
7. Если подходящего поставщика нет, ставит статус `Требует ручного разбора`.

## Seed-данные

При первом запуске backend проверяет БД. Если таблицы поставщиков и заказов пустые, он загружает данные из Excel-файлов в папке `data`.

По умолчанию используются:

- `../data/Таблица 1.xlsx`
- `../data/Таблица 2.xlsx`

Пути можно переопределить переменными окружения:

```bash
SUPPLIERS_XLSX=/path/to/suppliers.xlsx ORDERS_XLSX=/path/to/orders.xlsx ./gradlew bootRun
```

## Обновление данных

Frontend автоматически обновляет поставщиков, заказы и финансы каждые 15 секунд. Также в интерфейсе есть ручная кнопка обновления.
