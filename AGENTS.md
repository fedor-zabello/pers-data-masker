# AGENTS.md

Прокси-сервис маскирования/демаскирования персональных данных (ПД) перед LLM.
Java 21, Spring Boot 4.1.1, Maven. Пакет: `ru.cs.pers_data_masker`.

## Команды

- Сборка и тесты: `./mvnw test`
- Запуск: `./mvnw spring-boot:run` (порт 8080)
- Docker: `docker compose up --build`

## Критичные факты

- **Реальный URL ≠ `/process`.** В `application.yaml` задан
  `server.servlet.context-path: /persmasker`, поэтому эндпоинт доступен как
  `POST /persmasker/process`, а не `/process` (README устарел). При изменении
  конфига не ломай контракт нагрузочного теста.
- **`application.yaml` сейчас сломан**: `spring.threads.virtual.enabled:` без
  значения (пусто). Виртуальные потоки фактически не включены. Если чинишь —
  поставь `true`.
- **Masker работает слева направо с накопительным offset** (`Masker.java`): спаны
  сортируются по `start` по возрастанию, каждая замена переменной длины сдвигает
  последующие позиции, поэтому координаты фрагмента считаются как
  `span.start() + offset`. Не меняй на «справа налево» — это ломало координаты
  `MaskFragment` при нескольких спанах (демаскирование давало мусор/500).
- **`Span` ≠ `MaskFragment`.** `Span.start/end` — координаты в исходном тексте;
  `MaskFragment.maskStart/maskEnd` — координаты в замаскированной строке. В
  `CorrelationStore` хранятся только `MaskFragment` (маска почти всегда другой
  длины, чем оригинал). `Demasker` собирает слева направо по `maskStart`.
- **Тип ПД — `String`, не enum.** `Span`/`MaskFragment` хранят `type` как строку:
  встроенные — `PiiType.name()`, кастомные — `name` из YAML. `PiiType` — enum
  только для встроенных типов.
- **Кастомные типы ПД добавляются без Java-кода** — записью в
  `pii.custom-types` (regex + правило маскирования + приоритет) и включением в
  `pii.systems.<system>.types`. Валидация fail-fast при старте
  (`CustomPiiTypeRegistry`): компиляция regex, уникальность имён, целостность
  `systems.*.types` (`PiiTypeConfiguration`).
- **Новый встроенный тип ПД** = новый класс-детектор, реализующий `PiiDetector`
  (`detect`, `type`, `priority`), `@Component`. Spring собирает все в
  `List<PiiDetector>` через `DetectorOrchestrator`. Ядро не меняется.
- **Безопасность логов**: значения ПД никогда не логируются — только метаданные
  (`payload_id`, типы, счётчики, позиции). DEBUG — только замаскированный текст.
- **Security**: все запросы `permitAll`, идентификация системы — через заголовок
  `X-System-Id` в `SystemIdentificationFilter` (атрибут
  `SystemIdentificationFilter.SYSTEM_ID_ATTRIBUTE`). Проверка «система включена»
  идёт в фильтре, не в Security.

## Архитектура

Слои (пакеты): `api` (контроллер/ошибки) → `service` (`ProcessService`,
`BackpressureGuard`) → `detector` (детекторы + `DetectorOrchestrator` +
`SpanConflictResolver`) → `masking` (`Masker`, `Demasker`, стратегии) →
`store` (`CorrelationStore`, Caffeine TTL 10 мин) → `config` (properties,
резолверы) → `security` → `observability` (`MetricsService`).

`ProcessService.process` определяет направление по наличию `payload_id` в сторе:
нет → маскирование, есть → демаскирование (если системе разрешено). Идемпотентно
к ретраям.

## Документация (источник истины)

- `docs/SPECIFICATION.md` — архитектура и контракт.
- `docs/PLAN.md` — план работ со статусами `[ ]`/`[x]`; сверяйся при добавлении
  фич.
- `docs/YAML_PII_TYPES.md` — дизайн кастомных типов ПД.
- `docs/task/ds.txt` — ТЗ трека (контракт, критерии проверки, нагрузочный тест).
- `docs/PyJa-deprecated/` — устаревшие планы, не использовать.

## Тесты

Сейчас только `PersDataMaskerApplicationTests` (context loads). План
(`docs/PLAN.md` этап 11) требует unit-тесты на каждый детектор, маскирование и
`SpanConflictResolver`, плюс интеграционный тест цикла mask→unmask по `payload_id`
через MockMvc. При добавлении детектора/стратегии добавляй тесты.

### Тесты в Bruno

Тесты для коллекций Bruno добавляются в блок `runtime.scripts` с `type: tests`.
Код — JS-функции `test(...)` с `expect(...)`. Доступны `res.getStatus()` и
`res.getBody()`. Пример:

```yaml
runtime:
  scripts:
    - type: tests
      code: |-
        test("should be able to login", function () {
          expect(res.getStatus()).to.equal(200);
        });

        test("should return json", function () {
          expect(res.getBody()).to.eql({
            result: "Клиент И. И. И. обратился в отделение банка.",
          });
        });
```

Правила:
- Один `test(...)` на проверку; имя — человекочитаемое описание ожидания.
- `res.getStatus()` — HTTP-статус, `res.getBody()` — тело ответа (объект).
- Для сравнения тела целиком используй `expect(res.getBody()).to.eql({...})`.
- При добавлении/изменении эндпоинта добавляй/обновляй соответствующие тесты в
  коллекции Bruno.