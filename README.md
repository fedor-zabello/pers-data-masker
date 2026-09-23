# pers-data-masker

Сервис-прокси между системой-потребителем и LLM: идентифицирует персональные
данные (ПД) в тексте, маскирует их перед отправкой в LLM и демаскирует ответ.

Реализует единый контракт `POST /persmasker/process` для нагрузочного
тестирования. Обработка CPU-bound (regex) выполняется на виртуальных потоках
(`spring.threads.virtual.enabled=true`).

---

## Требования

- **Java 21** (JDK 21+)
- **Maven 3.9+**
- (опционально) **Docker** для запуска в контейнере

---

## Запуск

### Локально (Maven Wrapper)

```bash
# 1. Сборка и запуск тестов
./mvnw test

# 2. Запуск приложения
./mvnw spring-boot:run
```

Приложение стартует на `http://localhost:8080`, эндпоинт доступен по пути
`/persmasker/process` (задаётся `server.servlet.context-path` в
`application.yaml`).

### Docker

```bash
docker compose up --build
```

---

## Конфигурация

Основная конфигурация — `src/main/resources/application.yaml`.

### Профили систем

Каждая система-потребитель идентифицируется заголовком `X-System-Id`. Для
каждой системы настраивается: включена ли она, разрешено ли демаскирование,
и какие типы ПД обрабатывать.

```yaml
pii:
  systems:
    default:            # применяется, если заголовок X-System-Id отсутствует
      enabled: true
      demasking: true
      types: [FULL_NAME, PASSPORT, EMAIL, PHONE, INN, CARD_NUMBER, ...]
    system-a:
      enabled: true
      demasking: false
      types: [EMAIL, PHONE]
```

### Кастомные типы ПД (без Java-кода)

Новые типы ПД добавляются записью в `pii.custom-types` — regex для поиска,
правило маскирования и приоритет:

```yaml
pii:
  custom-types:
    - name: SNILS
      pattern: '\b\d{3}-\d{3}-\d{3} \d{2}\b'
      priority: 50
      masking:
        keep-start: 3        # оставить N символов в начале
        keep-end: 2          # оставить N символов в конце
        mask-char: '*'       # символ маскирования
        preserve-separators: true   # сохранять разделители (-, пробел)
```

После добавления тип доступен по имени в `systems.*.types`.

---

## Использование API

### Контракт

```
POST /persmasker/process
Content-Type: application/json

{ "payload": "<строка>", "payload_id": "<идентификатор>" }
  → 200
{ "result": "<строка>" }
```

Логика (один эндпоинт обрабатывает оба направления):

- **Первый запрос с новым `payload_id`** — маскирование. `payload` = исходная
  строка. Сервис возвращает маску и запоминает соответствие по `payload_id`.
- **Второй запрос с тем же `payload_id`** — демаскирование. `payload` = ранее
  возвращённая маска. Сервис возвращает исходную строку.

### Примеры запросов (curl)

#### 1. Маскирование

```bash
curl -X POST http://localhost:8080/persmasker/process \
  -H "Content-Type: application/json" \
  -d '{"payload":"Иванов Иван Иванович, email ivan@mail.ru, тел +7 (900) 123-45-67","payload_id":"req-001"}'
```

**Ответ:**

```json
{"result":"И. И. И., email i***@mail.ru, тел +7 (9**) ***-**-**"}
```

#### 2. Демаскирование (тот же `payload_id`)

```bash
curl -X POST http://localhost:8080/persmasker/process \
  -H "Content-Type: application/json" \
  -d '{"payload":"И. И. И., email i***@mail.ru, тел +7 (9**) ***-**-**","payload_id":"req-001"}'
```

**Ответ:**

```json
{"result":"Иванов Иван Иванович, email ivan@mail.ru, тел +7 (900) 123-45-67"}
```

#### 3. Идентификация системы через заголовок

```bash
curl -X POST http://localhost:8080/persmasker/process \
  -H "Content-Type: application/json" \
  -H "X-System-Id: system-a" \
  -d '{"payload":"ivan@mail.ru","payload_id":"req-002"}'
```

---

## Примеры датасетов и ответов

### Датасет 1. ФИО

| Вход | Маска |
|------|-------|
| `Иванов Иван Иванович` | `И. И. И.` |
| `Петрова Анна Сергеевна` | `П. А. С.` |

### Датасет 2. Email

| Вход | Маска |
|------|-------|
| `ivan@mail.ru` | `i***@mail.ru` |
| `petrov@yandex.ru` | `p***@yandex.ru` |

### Датасет 3. Телефон

| Вход | Маска |
|------|-------|
| `+7 (900) 123-45-67` | `+7 (9**) ***-**-**` |
| `8 900 123 45 67` | `8 9** *** ** **` |

### Датасет 4. Номер карты

| Вход | Маска |
|------|-------|
| `1234 5678 9012 3456` | `1234 **** **** 3456` |

### Датасет 5. ИНН

| Вход | Маска |
|------|-------|
| `7707083893` | `7707****93` |

### Датасет 6. Паспорт

| Вход | Маска |
|------|-------|
| `серия 4509 номер 123456` | `серия 45** номер ****56` |

### Датасет 7. Кастомный тип (СНИЛС)

| Вход | Маска |
|------|-------|
| `123-456-789 01` | `123-***-*** **` |

---

## Коды ответов

| Код | Условие |
|-----|---------|
| 200 | Успешная обработка |
| 400 | Ошибка валидации запроса |
| 403 | Система отключена или демаскирование запрещено |
| 429 | Перегрузка (заголовок `Retry-After`) |
| 500 | Внутренняя ошибка |

---

## Метрики

Prometheus-метрики доступны на `http://localhost:8080/actuator/prometheus`:

- `pii.requests` — счётчик запросов (для RPS/TPS);
- `pii.process.latency` — гистограмма latency;
- `pii.detected` — счётчик найденных ПД по типам (тег `type`).

---

## Тесты

```bash
./mvnw test
```

Покрытие: unit-тесты детекторов, маскирования и разрешения пересечений,
интеграционный тест цикла mask→unmask по `payload_id`.

---

## Документация

- [SPECIFICATION.md](docs/SPECIFICATION.md) — архитектура и контракт.
- [PLAN.md](docs/PLAN.md) — план работ.
- [YAML_PII_TYPES.md](docs/YAML_PII_TYPES.md) — дизайн настраиваемых типов ПД.