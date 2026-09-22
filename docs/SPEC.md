# Спецификация решения: Модуль безопасности персональных данных

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

## 2. Компоненты

### Java-сервис (Spring Boot)
- **API `/process`**: `POST {payload, payload_id}` → `{result}`
- **Auth**: проверка `X-API-Key` в каждом запросе, неизвестный → 401
- **RateLimiter**: по системе, лимит в YAML, превышение → 429 + Retry-After
- **CorrelationStore** (интерфейс): `InMemoryImpl` (ConcurrentHashMap, TTL 10 мин), будущий `RedisImpl`
- **LocalRegexFallback**: резервные regex-детекторы при недоступности Python
- **SpanConflictResolver**: разрешение конфликтов спанов по приоритету типов (YAML)
- **CombinationRules**: правила-комбинации (PIN+карта и т.п.) в YAML
- **SafeLogging**: логирует только типы/количество ПД, не значения
- **Metrics**: Micrometer + Prometheus (RPS, latency, TPS)
- **Конфигурация**: YAML + hot-reload

### Python-микросервис (FastAPI + gRPC)
- **Natasha NER**: ФИО, даты, адреса
- **Regex-детекторы**: из YAML-конфигурации (паспорт, ИНН, email, телефон, карта, разделяющие слова)
- **Контекст**: маркеры + confidence (0..1)
- **Нормализация**: нижний регистр для детекции, позиции по исходному тексту
- **Даты**: regex (числовые) + Natasha (текстовые)
- **Протокол**: gRPC, один запрос целиком (до 100k токенов)

## 3. Формат данных

### gRPC (Java ↔ Python)
```
DetectRequest { text }
DetectResponse { spans: [{start, end, type, confidence}] }
```

### CorrelationStore (вариант 1 — маппинг фрагментов)
```
payload_id → {
    fragments: [
        {mask, original, maskStart, maskEnd}
    ]
}
```

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

## 5. Маскирование
- **Формат**: частичное (паспорт `45** ****56`, ФИО `И. И. И.`)
- **Порядок**: справа налево (позиции не сдвигаются)
- **Фильтрация**: по `maskTypes` системы

## 6. Демаскирование
- **По store**: payload игнорируем
- **Сборка**: сортировка фрагментов по `maskStart`, сборка слева направо
- **Пустые фрагменты**: вернуть payload как есть

## 7. Конфигурация (YAML)

```yaml
# systems.yaml
systems:
  - id: "system-a"
    apiKey: "key-a-123"
    maskTypes: [PERSON, PASSPORT]
    rateLimit: 1000

# detectors.yaml (Python)
regexDetectors:
  - type: PASSPORT
    pattern: "\\d{4}\\s?\\d{6}"
  - type: SNILS
    pattern: "\\d{3}-\\d{3}-\\d{3} \\d{2}"

# priorities.yaml (Java)
spanPriorities:
  PASSPORT: 100
  PERSON: 50
  ADDRESS: 30

# combinations.yaml (Java)
combinations:
  - mask: PIN
    onlyIf: [CARD]
```

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

## 9. Безопасность
- **SafeLogging**: только типы/количество ПД, не значения
- **Auth**: X-API-Key
- **Fallback**: LocalRegexFallback при недоступности Python

## 10. Метрики
- Micrometer + Prometheus: RPS, latency histogram, TPS, типы ПД