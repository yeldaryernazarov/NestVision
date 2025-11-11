# NestVision MVP

Система для просмотра видеозаписей с камер детского сада.

## Структура проекта

```
NestVision/
├── backend/          # Spring Boot приложение (Java 24)
├── frontend/         # Next.js приложение (TypeScript)
└── MVP_IMPLEMENTATION_PLAN.md
```

## Технологический стек

- **Backend**: Java 24, Spring Boot 3.3.0, PostgreSQL
- **Frontend**: Next.js 16, TypeScript, Tailwind CSS
- **База данных**: PostgreSQL
- **Хранилище видео**: Локальная папка `./videos` в backend

## Быстрый старт

### Предварительные требования

- Java 24 (или совместимая версия)
- Maven 3.8+
- Node.js 20+ (рекомендуется, но может работать с 16+)
- PostgreSQL 14+

### Настройка базы данных

1. Создайте базу данных PostgreSQL:
```sql
CREATE DATABASE nestvision_db;
```

2. Обновите credentials в `backend/src/main/resources/application.properties`:
```properties
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### Запуск Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

Backend будет доступен на `http://localhost:8080`

### Запуск Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend будет доступен на `http://localhost:3000`

## Конфигурация

### Backend

Основные настройки в `backend/src/main/resources/application.properties`:
- Порт: 8080
- База данных: PostgreSQL (localhost:5432/nestvision_db)
- Хранилище видео: `./videos` (создается автоматически)

### Frontend

API URL настраивается через переменную окружения:
```bash
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

Или в `.env.local`:
```
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

## Структура API

### Аутентификация
- `POST /api/auth/register` - Регистрация
- `POST /api/auth/login` - Вход
- `GET /api/auth/me` - Текущий пользователь

### Видео
- `GET /api/videos` - Все видео
- `GET /api/videos/category/{category}` - Видео по категории
- `GET /api/videos/{id}` - Информация о видео
- `GET /api/videos/{id}/stream` - Поток видео

## Категории видео

1. AGGRESSION_BETWEEN_CHILDREN - Агрессия между детьми
2. AGGRESSION_TEACHER - Агрессия воспитателя
3. SUDDEN_EVENT - Внезапное событие
4. CHILDREN_UNATTENDED - Дети без присмотра

## Разработка

Следуйте плану в `MVP_IMPLEMENTATION_PLAN.md` для пошаговой реализации.

## Примечания

- Это MVP версия, тесты не включены
- Видео хранятся локально в папке `backend/videos`
- JWT токены используются для аутентификации
- CORS настроен для `http://localhost:3000`

