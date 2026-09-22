# Дизайн: настраиваемые типы ПД через YAML

## 1. Цель

Дать возможность добавлять новые типы персональных данных (ПД) для
маскирования/демаскирования **без написания Java-кода** — только через
конфигурацию `application.yaml`.

Новый тип ПД = запись в YAML с:
- **regex-паттерном** для идентификации в тексте;
- **правилом маскирования** (гибкие правила);
- **приоритетом** для разрешения пересечений спанов;
- **именем типа** (для метрик и логов).

Демаскирование работает через существующий `CorrelationStore` (маска→оригинал
по `payload_id`), как и для встроенных типов.

---

## 2. Модель конфигурации

```yaml
pii:
  custom-types:
    - name: SNILS
      pattern: '\b\d{3}-\d{3}-\d{3} \d{2}\b'
      priority: 50
      masking:
        keep-start: 3        # сколько символов оставить в начале
        keep-end: 2          # сколько символов оставить в конце
        mask-char: '*'       # чем заменить середину
        preserve-separators: true   # сохранять разделители (-, пробелы)
    - name: DRIVER_LICENSE
      pattern: '\b\d{2} \d{2} \d{6}\b'
      priority: 40
      masking:
        keep-start: 0
        keep-end: 0
        mask-char: '*'
```

### Поля

| Поле | Тип | Обязательно | Описание |
|------|-----|-------------|----------|
| `name` | string | да | Имя типа ПД (для метрик, логов, ссылок из профилей систем) |
| `pattern` | string | да | Regex для идентификации (Java `Pattern`, регистронезависимый) |
| `priority` | int | нет (default 0) | Приоритет при пересечении спанов (больше = важнее) |
| `masking.keep-start` | int | нет (default 0) | Сколько символов оставить в начале фрагмента |
| `masking.keep-end` | int | нет (default 0) | Сколько символов оставить в конце фрагмента |
| `masking.mask-char` | string | нет (default `*`) | Символ маскирования |
| `masking.preserve-separators` | bool | нет (default false) | Сохранять не-буквенно-цифровые символы (разделители) |

---

## 3. Архитектура

### 3.1. Новые компоненты

```
application.yaml
      │  (@ConfigurationProperties)
      ▼
CustomPiiTypeProperties (List<CustomPiiTypeConfig>)
      │
      ▼
CustomPiiTypeRegistry  ──►  CustomPiiDetector (по одному на тип)
      │                          │  implements PiiDetector
      │                          ▼
      │                    RegexPiiDetector (общий движок)
      │
      └──►  CustomMaskingStrategy (по одному на тип)
                    │  implements MaskingStrategy
                    ▼
              FlexibleMaskingStrategy (общий движок)
```

### 3.2. Поток данных

1. `CustomPiiTypeProperties` — `@ConfigurationProperties(prefix = "pii.custom-types")`,
   читает список типов из YAML.
2. `CustomPiiTypeRegistry` — собирает типы, валидирует (компилирует regex,
   проверяет уникальность `name`), предоставляет доступ по имени.
3. Для каждого типа создаются два бина:
   - `CustomPiiDetector` — оборачивает `RegexPiiDetector` (компилирует `pattern`,
     возвращает `Span`-ы с типом и приоритетом);
   - `CustomMaskingStrategy` — оборачивает `FlexibleMaskingStrategy`
     (применяет `keep-start`/`keep-end`/`mask-char`/`preserve-separators`).
4. `DetectorOrchestrator` собирает **все** `PiiDetector` (встроенные + кастомные)
   через `List<PiiDetector>`, фильтрует по типам системы и разрешает пересечения
   через `SpanConflictResolver` (по `priority()`).
5. `Masker` применяет `MaskingStrategy` для каждого спана (по типу), строит маску
   справа налево и формирует `MaskFragment`-ы.
6. `Demasker` восстанавливает оригинал из `MaskFragment`-ов из `CorrelationStore`.

### 3.3. Интеграция с существующими абстракциями

- `CustomPiiDetector` реализует существующий интерфейс `PiiDetector`
  (`detect`, `type`, `priority`) — оркестратор не знает, встроенный это тип
  или кастомный.
- `CustomMaskingStrategy` реализует существующий интерфейс `MaskingStrategy` —
  `Masker` выбирает стратегию по типу спана.
- `PiiType` — для кастомных типов используется динамическое представление
  (см. §4), чтобы не менять enum при каждом новом типе.

---

## 4. Представление типа ПД

Встроенные типы — enum `PiiType`. Кастомные типы не могут быть элементами enum
(enum фиксирован на этапе компиляции). Решение:

- Ввести интерфейс `PiiTypeRef` (или использовать `String`-имя типа).
- `Span` и `MaskFragment` хранят тип как `String` (имя) + приоритет.
- Встроенные типы мапятся на свои имена enum; кастомные — на `name` из YAML.
- Метрики/логи используют строковое имя типа.

**Альтернатива (проще):** хранить тип в `Span`/`MaskFragment` как `String`.
Встроенные детекторы возвращают `type().name()`. Это минимально меняет модель.

---

## 5. Правила маскирования (FlexibleMaskingStrategy)

Для фрагмента `original` длиной `L`:

1. Если `L <= keep-start + keep-end` → маскировать всё (или оставить как есть,
   настраивается).
2. Иначе:
   - оставить первые `keep-start` символов;
   - оставить последние `keep-end` символов;
   - середину заменить на `mask-char` (повторённым нужное число раз);
   - если `preserve-separators=true` — не-буквенно-цифровые символы в середине
     сохраняются, маскируются только буквы/цифры.

Примеры (для `SNILS` `123-456-789 01`, `keep-start=3`, `keep-end=2`,
`preserve-separators=true`):
- `123-456-789 01` → `123-***-*** **` (разделители `-` и пробел сохранены).

---

## 6. Валидация конфигурации

При старте приложения `CustomPiiTypeRegistry` проверяет:
- `name` не пустой и уникальный;
- `pattern` компилируется (иначе `IllegalArgumentException` при старте);
- `keep-start`/`keep-end` ≥ 0;
- `mask-char` — один символ (или строка, повторяемая).

Ошибка конфигурации → приложение не стартует (fail-fast), чтобы не было
«тихих» неработающих типов.

---

## 7. Профили систем

Кастомные типы подключаются к системам так же, как встроенные — по имени в
`pii.systems.<system>.types`:

```yaml
pii:
  custom-types:
    - name: SNILS
      pattern: '\b\d{3}-\d{3}-\d{3} \d{2}\b'
      priority: 50
      masking: { keep-start: 3, keep-end: 2, preserve-separators: true }
  systems:
    default:
      enabled: true
      demasking: true
      types: [FULL_NAME, PASSPORT, EMAIL, PHONE, SNILS]
```

`SystemConfigResolver` резолвит имя типа → детектор/стратегия через реестр.

---

## 8. Метрики и логи

- Счётчики найденных ПД по типам используют строковое имя (`SNILS`, ...) —
  кастомные типы автоматически появляются в метриках.
- Логи — только метаданные (имя типа, количество, позиции), значения ПД не
  логируются (как в спецификации).

---

## 9. Что НЕ входит (границы)

- Сложные контекстные проверки (словари исключений) — остаются в Java-детекторах.
- Многострочные/перекрывающиеся паттерны — ограничение regex-подхода.
- Динамическое добавление типов в рантайме (без рестарта) — вне базовой версии.

---

## 10. Затрагиваемые файлы

| Файл | Изменение |
|------|-----------|
| `pom.xml` | (без изменений) |
| `application.yaml` | добавить `pii.custom-types` |
| `CustomPiiTypeProperties` | новый `@ConfigurationProperties` |
| `CustomPiiTypeRegistry` | новый: валидация + доступ по имени |
| `RegexPiiDetector` | новый: общий regex-движок |
| `CustomPiiDetector` | новый: `PiiDetector` для кастомного типа |
| `FlexibleMaskingStrategy` | новый: общий движок маскирования |
| `CustomMaskingStrategy` | новый: `MaskingStrategy` для кастомного типа |
| `PiiType` / `Span` / `MaskFragment` | тип как `String` (или `PiiTypeRef`) |
| `DetectorOrchestrator` | собирает кастомные детекторы (через `List<PiiDetector>`) |
| `Masker` | выбор стратегии по типу (в т.ч. кастомной) |
| `SystemConfigResolver` | резолв кастомных типов по имени |

---

## 11. Пример использования

Добавить новый тип ПД (например, СНИЛС) — только YAML:

```yaml
pii:
  custom-types:
    - name: SNILS
      pattern: '\b\d{3}-\d{3}-\d{3} \d{2}\b'
      priority: 50
      masking:
        keep-start: 3
        keep-end: 2
        mask-char: '*'
        preserve-separators: true
```

Перезапуск → тип `SNILS` доступен для маскирования/демаскирования, попадает в
метрики и может быть включён в профили систем. Java-код не меняется.