# Детальная спецификация решения: Модуль безопасности персональных данных

## 1. Общая архитектура

```
┌──────────────┐     ┌──────────────────────┐     ┌──────────────────┐
│  Потребитель  │────▶│  Java-сервис          │────▶│  Python NLP      │
│  (система)    │     │  (Spring Boot)        │     │  (FastAPI+gRPC)  │
│               │◀────│  - API /process       │◀────│  - Natasha NER   │
└──────────────┘     │  - маскирование       │     │  - regex (YAML)  │
                     │  - корреляция         │     │  - контекст/confidence
                     │  - конфигурация       │     │  - нормализация
                     │    (YAML + hot-reload)│     │  - даты (regex+Natasha)
                     │  - CorrelationStore   │     │  - разделяющие слова
                     │    (TTL 10 мин)       │     └──────────────────┘
                     │  - LocalRegexFallback │
                     │  - Idempotency check  │
                     │  - RateLimiter        │
                     │  - SafeLogging        │
                     │  - Metrics            │
                     │  - CombinationRules   │
                     │  - SpanConflictResolver
                     │  - Auth (X-API-Key)
                     └──────────────────────┘
```

### Поток данных

1. Потребитель (система) отправляет `POST /process` с `{payload, payload_id}` и заголовком `X-API-Key`.
2. Java-сервис аутентифицирует систему, применяет rate limiting.
3. Java определяет направление по наличию `payload_id` в CorrelationStore.
4. При маскировании: Java отправляет текст в Python по gRPC, получает спаны, разрешает конфликты, фильтрует по `maskTypes`, маскирует, сохраняет фрагменты.
5. При демаскировании: Java собирает исходник из сохранённых фрагментов.
6. Java возвращает `{result}`.

---

## 2. Компоненты

### 2.1 Java-сервис (Spring Boot)

| Компонент | Ответственность | Ключевые детали |
|-----------|----------------|-----------------|
| `ProcessController` | HTTP-эндпоинт `POST /process` | Принимает `{payload, payload_id}`, возвращает `{result}` |
| `ApiKeyFilter` | Аутентификация | Проверяет `X-API-Key`, неизвестный → 401 |
| `SystemRegistry` | Реестр систем | id, apiKey, maskTypes, rateLimit; hot-reload |
| `SystemRateLimiter` | Rate limiting | По системе, лимит из конфигурации, превышение → 429 + Retry-After |
| `CorrelationStore` | Хранение соответствий | Интерфейс; `InMemoryImpl` (TTL 10 мин), будущий `RedisImpl` |
| `DetectorClient` | gRPC-клиент к Python | Таймаут ~500 мс, обработка недоступности |
| `LocalRegexFallback` | Резервная детекция | Regex при недоступности Python |
| `SpanConflictResolver` | Разрешение конфликтов спанов | По приоритету типов из YAML |
| `MaskingEngine` | Маскирование | Фильтрация по maskTypes, справа налево, сохранение фрагментов |
| `DemaskingEngine` | Демаскирование | Сборка исходника по фрагментам |
| `CombinationRules` | Правила-комбинации | PIN+карта и т.п. |
| `SafeLogger` | Безопасное логирование | Только типы/количество ПД, не значения |
| `MetricsService` | Метрики | Micrometer + Prometheus: RPS, latency, TPS |

### 2.2 Python-микросервис (FastAPI + gRPC)

| Компонент | Ответственность | Ключевые детали |
|-----------|----------------|-----------------|
| `DetectService` | gRPC-сервер | Принимает текст, возвращает спаны |
| `RegexDetector` | Regex-детекция | Из YAML-правил (паспорт, ИНН, email, телефон, карта) |
| `NatashaDetector` | NER-детекция | ФИО, даты, адреса, места рождения |
| `ContextAnalyzer` | Контекст | Маркеры + confidence (0..1) |
| `Normalizer` | Нормализация | Нижний регистр для детекции, позиции по исходному тексту |
| `DetectorRegistry` | Реестр детекторов | Регистрация и объединение |

---

## 3. Формат данных

### 3.1 gRPC (Java ↔ Python)

```proto
syntax = "proto3";

package detect;

service DetectService {
  rpc Detect (DetectRequest) returns (DetectResponse);
}

message DetectRequest {
  string text = 1;
}

message Span {
  int32 start = 1;
  int32 end = 2;
  string type = 3;
  float confidence = 4;
}

message DetectResponse {
  repeated Span spans = 1;
}
```

### 3.2 CorrelationStore (вариант 1 — маппинг фрагментов)

```
payload_id → {
    fragments: [
        {
            mask: "И. И. И.",              // замаскированный фрагмент
            original: "Иванов Иван Иванович", // исходный фрагмент
            maskStart: 8,                  // позиция в маске (start)
            maskEnd: 17                    // позиция в маске (end)
        }
    ]
}
```

### 3.3 HTTP-контракт `/process`

**Запрос:**
```json
{
  "payload": "Клиент Иванов Иван Иванович, паспорт 4509 123456",
  "payload_id": "8a77d363c7c044b49b41d7b8a448243a"
}
```

**Ответ (200):**
```json
{
  "result": "Клиент И. И. И., паспорт 45** ****56"
}
```

**Ошибки:**
- `401` — невалидный `X-API-Key`
- `429` — превышен rate limit (+ `Retry-After`)
- `5xx` — внутренняя ошибка

---

## 4. Логика `/process` (финальная)

```
Запрос {payload, payload_id}
  │
  ├─ Аутентификация (X-API-Key)
  │     ├─ ключ невалиден → 401
  │     └─ ключ валиден → определить систему
  │
  ├─ Rate limiting по системе
  │     └─ превышен → 429 + Retry-After
  │
  ├─ payload_id ЕСТЬ в store?
  │     ├─ ДА → ДЕМАСКИРОВАНИЕ
  │     │      ├─ фрагменты ЕСТЬ?
  │     │      │     ├─ ДА → собрать исходник (по store, payload игнорируем)
  │     │      │     └─ НЕТ → вернуть payload как есть
  │     │      └─ вернуть результат
  │     │
  │     └─ НЕТ → МАСКИРОВАНИЕ
  │            → детекция → спаны
  │            → разрешить конфликты
  │            → отфильтровать по maskTypes системы
  │            → спаны остались?
  │                  ├─ ДА → маскировать (справа налево) → сохранить фрагменты → вернуть маску
  │                  └─ НЕТ → вернуть текст как есть → сохранить пустые фрагменты
```

---

## 5. Маскирование

### 5.1 Формат (частичное маскирование)

| Тип ПД | Исходное | Маска |
|--------|----------|-------|
| `PERSON` | Иванов Иван Иванович | И. И. И. |
| `PASSPORT` | 4509 123456 | 45** ****56 |
| `EMAIL` | ivan@mail.ru | i***@mail.ru |
| `PHONE` | +7 (900) 123-45-67 | +7 (9**) ***-**-** |
| `CARD` | 1234 5678 9012 3456 | 1234 **** **** 3456 |
| `INN` | 123456789012 | 1234******12 |

### 5.2 Алгоритм

1. Получить спаны от Python (или LocalRegexFallback).
2. Разрешить конфликты (SpanConflictResolver).
3. Отфильтровать по `maskTypes` системы.
4. Отсортировать спаны по `start` (убывание).
5. Маскировать справа налево (позиции не сдвигаются).
6. Сохранить фрагменты с `maskStart`/`maskEnd` в маске.

---

## 6. Демаскирование

### 6.1 Алгоритм

1. Получить фрагменты из store по `payload_id`.
2. Если фрагментов нет → вернуть payload как есть.
3. Отсортировать фрагменты по `maskStart` (возрастание).
4. Собрать слева направо:
   - Копировать текст до `maskStart` → в результат.
   - Вставить `original` → в результат.
   - Пропустить до `maskEnd`.
5. Вернуть собранный исходник.

### 6.2 Пример

```
Маска: "Клиент И. И. И., паспорт 45** ****56"
Фрагменты:
  [0] maskStart=8,  maskEnd=17, original="Иванов Иван Иванович"
  [1] maskStart=27, maskEnd=38, original="4509 123456"

Результат:
  "Клиент " + "Иванов Иван Иванович" + ", паспорт " + "4509 123456"
  = "Клиент Иванов Иван Иванович, паспорт 4509 123456"
```

---

## 7. Конфигурация (YAML)

### 7.1 systems.yaml (Java)

```yaml
systems:
  - id: "system-a"
    apiKey: "key-a-123"
    maskTypes: [PERSON, PASSPORT]
    rateLimit: 1000
  - id: "system-b"
    apiKey: "key-b-456"
    maskTypes: [PERSON, PASSPORT, CARD, EMAIL]
    rateLimit: 2000
```

### 7.2 detectors.yaml (Python)

```yaml
regexDetectors:
  - type: PASSPORT
    pattern: "\\d{4}\\s?\\d{6}"
  - type: PASSPORT_SEPARATED
    pattern: "серия\\s+(\\d{4})\\s+номер\\s+(\\d{6})"
  - type: INN
    pattern: "\\d{12}"
  - type: EMAIL
    pattern: "[\\w.]+@[\\w.]+"
  - type: PHONE
    pattern: "\\+7\\s?\\(\\d{3}\\)\\s?\\d{3}-\\d{2}-\\d{2}"
  - type: CARD
    pattern: "\\d{4}\\s?\\d{4}\\s?\\d{4}\\s?\\d{4}"
  - type: CVV
    pattern: "\\d{3}"
  - type: DIVISION_CODE
    pattern: "\\d{3}-\\d{3}"

contextMarkers:
  PERSON: ["клиент", "гр", "родился", "проживает", "заявитель"]
  BIRTH_PLACE: ["родился в", "место рождения"]
  PASSPORT_ISSUER: ["выдан", "кем выдан"]
  PIN: ["пин", "пин-код"]
  CVV: ["cvv", "код"]
```

### 7.3 priorities.yaml (Java)

```yaml
spanPriorities:
  PASSPORT: 100
  CARD: 90
  INN: 80
  PERSON: 50
  ADDRESS: 30
  EMAIL: 20
  PHONE: 20
```

### 7.4 combinations.yaml (Java)

```yaml
combinations:
  - mask: PIN
    onlyIf: [CARD]
```

---

## 8. Категории ПД и варианты парсинга

| Категория ПД | Тип (span) | Варианты парсинга | Источник |
|--------------|-----------|-------------------|----------|
| ФИО | `PERSON` | Natasha NER; контекстные маркеры («клиент», «гр.», «родился») | Python (Natasha) |
| Дата рождения | `BIRTH_DATE` | regex `\d{2}\.\d{2}\.\d{4}`, `\d{4}\.\d{2}\.\d{2}`; Natasha (текстом) | Python (regex + Natasha) |
| Место рождения | `BIRTH_PLACE` | Natasha NER (локации); контекстные маркеры («родился в») | Python (Natasha) |
| Серия и номер паспорта | `PASSPORT` | regex `\d{4}\s?\d{6}`; разделяющие слова «серия … номер …» | Python (regex) |
| Гражданство | `CITIZENSHIP` | словарь стран; контекстные маркеры («гражданин», «гражданство») | Python (regex/словарь) |
| Орган, выдавший паспорт | `PASSPORT_ISSUER` | контекстные маркеры («выдан», «кем выдан»); словарь | Python (regex/словарь) |
| Код подразделения | `DIVISION_CODE` | regex `\d{3}-\d{3}` | Python (regex) |
| Дата выдачи паспорта | `PASSPORT_DATE` | regex дат; контекстные маркеры («выдан …») | Python (regex) |
| Серия и номер в/у | `DRIVER_LICENSE` | regex `\d{4}\s?\d{6}`; разделяющие слова | Python (regex) |
| Адрес | `ADDRESS` | Natasha NER (адреса); разбор по частям (страна, индекс, город, улица, дом, квартира) | Python (Natasha) |
| Email | `EMAIL` | regex `[\w.]+@[\w.]+` | Python (regex) |
| Номер телефона | `PHONE` | regex `\+7\s?\(\d{3}\)\s?\d{3}-\d{2}-\d{2}` и вариации | Python (regex) |
| ИНН | `INN` | regex `\d{12}` (физлицо), `\d{10}` (юрлицо) | Python (regex) |
| Номер платёжной карты | `CARD` | regex `\d{4}\s?\d{4}\s?\d{4}\s?\d{4}` | Python (regex) |
| CVV-код | `CVV` | regex `\d{3}`; контекстные маркеры («cvv», «код») | Python (regex) |
| Пин-код карты | `PIN` | regex `\d{4}`; контекстные маркеры («пин», «пин-код») | Python (regex) |
| Имя держателя карты | `CARD_HOLDER` | Natasha NER (PERSON); контекстные маркеры («holder», «держатель») | Python (Natasha) |

### Варианты парсинга по сложности

| Вариант | Описание | Плюсы | Минусы |
|---------|----------|-------|--------|
| **A. Regex + словари** | Только regex-шаблоны и словари (страны, маркеры) | Быстро, просто, предсказуемо | Не ловит ФИО/адреса/даты текстом |
| **B. Regex + Natasha** | Regex для форматов + Natasha для ФИО/дат/адресов | Покрывает большинство категорий, качество ~95% | Тяжелее, нужен Python-микросервис |
| **C. Regex + Natasha + ML** | Добавить ML-классификацию контекста | Максимальная точность | Сложно, избыточно для хакатона |

**Выбранный вариант: B** — regex (форматы) + Natasha (ФИО/даты/адреса), с контекстными маркерами и confidence.

---

## 9. Безопасность

- **SafeLogging**: логирует только типы/количество ПД, не значения.
  - Пример: `masked 2 PERSON, 1 PASSPORT` (не `masked "Иванов Иван"`).
- **Auth**: `X-API-Key` в каждом запросе, неизвестный → 401.
- **Fallback**: LocalRegexFallback при недоступности Python.
- **CorrelationStore**: данные в памяти, не в логах; TTL 10 мин.

---

## 10. Метрики (Micrometer + Prometheus)

| Метрика | Тип | Описание |
|---------|-----|----------|
| `masker_requests_total` | Counter | Общее число запросов (по системе, по направлению) |
| `masker_latency_seconds` | Histogram | Latency обработки |
| `masker_rps` | Gauge | Запросов в секунду |
| `masker_tps` | Gauge | Токенов в секунду |
| `masker_masked_by_type` | Counter | Количество замаскированных ПД по типу |

Эндпоинт: `/actuator/prometheus`.

---

## 11. Нефункциональные требования

| Требование | Значение | Как достигается |
|------------|----------|-----------------|
| Latency | ≤ 1 сек | Async, gRPC, таймаут 500 мс, маскирование справа налево |
| RPS | 1000 (бонус 2000) | Stateless, горизонтальное масштабирование, rate limiting |
| TPS | до 100k токенов | Один gRPC-запрос целиком |
| Качество | 95% | Regex + Natasha + контекст |
| Масштабируемость | Добавление типов ПД | Regex в YAML, детекторы в реестре |
| Надёжность | Деградация при недоступности | LocalRegexFallback, таймауты |