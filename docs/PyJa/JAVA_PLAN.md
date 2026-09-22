# План реализации Java-приложения (Spring Boot)

## 1. Структура проекта

```
java-app/
├── pom.xml
├── src/main/java/com/bank/masker/
│   ├── MaskerApplication.java
│   ├── config/
│   │   ├── AppConfig.java
│   │   ├── GrpcClientConfig.java
│   │   └── YmlConfigLoader.java
│   ├── api/
│   │   ├── ProcessController.java
│   │   ├── dto/
│   │   │   ├── ProcessRequest.java
│   │   │   └── ProcessResponse.java
│   │   └── exception/
│   │       ├── ApiExceptionHandler.java
│   │       └── UnauthorizedException.java
│   ├── auth/
│   │   ├── ApiKeyFilter.java
│   │   └── SystemRegistry.java
│   ├── ratelimit/
│   │   ├── RateLimiter.java
│   │   └── SystemRateLimiter.java
│   ├── correlation/
│   │   ├── CorrelationStore.java          (интерфейс)
│   │   ├── InMemoryCorrelationStore.java
│   │   ├── RedisCorrelationStore.java     (будущий)
│   │   └── model/
│   │       ├── CorrelationEntry.java
│   │       └── Fragment.java
│   ├── detection/
│   │   ├── Span.java
│   │   ├── SpanType.java
│   │   ├── DetectorClient.java           (gRPC → Python)
│   │   ├── LocalRegexFallback.java
│   │   └── SpanConflictResolver.java
│   ├── masking/
│   │   ├── MaskingEngine.java
│   │   ├── MaskingRule.java
│   │   ├── MaskingRuleRegistry.java
│   │   └── CombinationRules.java
│   ├── demasking/
│   │   └── DemaskingEngine.java
│   ├── logging/
│   │   └── SafeLogger.java
│   └── metrics/
│       └── MetricsService.java
└── src/main/resources/
    ├── application.yml
    ├── config/
    │   ├── systems.yaml
    │   ├── priorities.yaml
    │   └── combinations.yaml
    └── proto/detect.proto
```

## 2. Порядок реализации

### Этап 1: Каркас и конфигурация
- [ ] `pom.xml`: Spring Boot, Web, Validation, Micrometer, gRPC (grpc-netty-shaded, grpc-protobuf, grpc-stub), SnakeYAML
- [ ] `MaskerApplication.java` — точка входа
- [ ] `YmlConfigLoader` — загрузка `systems.yaml`, `priorities.yaml`, `combinations.yaml`
- [ ] `SystemRegistry` — реестр систем (id, apiKey, maskTypes, rateLimit)
- [ ] Hot-reload конфигурации (периодическое перечитывание YAML)

### Этап 2: API и аутентификация
- [ ] `ProcessController` — `POST /process`
- [ ] `ProcessRequest` / `ProcessResponse` DTO
- [ ] `ApiKeyFilter` — проверка `X-API-Key`, 401 при невалидном
- [ ] `ApiExceptionHandler` — обработка ошибок (401, 429, 5xx)

### Этап 3: Rate limiting
- [ ] `RateLimiter` — интерфейс
- [ ] `SystemRateLimiter` — по системе, лимит из конфигурации
- [ ] 429 + `Retry-After` при превышении

### Этап 4: CorrelationStore
- [ ] `CorrelationStore` — интерфейс (save, get, remove)
- [ ] `InMemoryCorrelationStore` — ConcurrentHashMap + TTL 10 мин
- [ ] `CorrelationEntry` / `Fragment` модели
- [ ] `RedisCorrelationStore` — заглушка/интерфейс для будущего

### Этап 5: Детекция (gRPC клиент)
- [ ] Сгенерировать классы из `detect.proto`
- [ ] `DetectorClient` — gRPC вызов к Python, таймаут ~500 мс
- [ ] `LocalRegexFallback` — резервные regex при недоступности Python
- [ ] `SpanConflictResolver` — разрешение конфликтов по приоритетам

### Этап 6: Маскирование
- [ ] `MaskingRule` — интерфейс (маска по типу ПД)
- [ ] `MaskingRuleRegistry` — реестр правил
- [ ] `MaskingEngine` — фильтрация по maskTypes, маскирование справа налево, сохранение фрагментов
- [ ] `CombinationRules` — правила-комбинации (PIN+карта)

### Этап 7: Демаскирование
- [ ] `DemaskingEngine` — сборка исходника по фрагментам (сортировка по maskStart, слева направо)

### Этап 8: Логирование и метрики
- [ ] `SafeLogger` — логирование только типов/количества ПД
- [ ] `MetricsService` — Micrometer: RPS, latency histogram, TPS, типы ПД

### Этап 9: Сборка логики `/process`
- [ ] Связать все компоненты в `ProcessController`
- [ ] Реализовать полную логику (маскирование/демаскирование по payload_id)

## 3. Логика `/process`

```
Запрос {payload, payload_id}
  ├─ Auth (X-API-Key) → 401 если невалиден
  ├─ RateLimit → 429 + Retry-After если превышен
  ├─ payload_id ЕСТЬ в store?
  │     ├─ ДА → ДЕМАСКИРОВАНИЕ
  │     │      ├─ фрагменты ЕСТЬ? → собрать исходник (по store)
  │     │      └─ НЕТ → вернуть payload как есть
  │     └─ НЕТ → МАСКИРОВАНИЕ
  │            → детекция (gRPC → Python, fallback на regex)
  │            → разрешить конфликты
  │            → отфильтровать по maskTypes
  │            → спаны остались?
  │                  ├─ ДА → маскировать (справа налево) → сохранить фрагменты → вернуть маску
  │                  └─ НЕТ → вернуть текст как есть → сохранить пустые фрагменты
```

## 4. Зависимости (pom.xml)

- `spring-boot-starter-web`
- `spring-boot-starter-validation`
- `spring-boot-starter-actuator`
- `io.micrometer:micrometer-registry-prometheus`
- `net.devh:grpc-client-spring-boot-starter` (или grpc-netty-shaded + grpc-stub)
- `org.yaml:snakeyaml`
- `com.google.protobuf:protobuf-java`

## 5. Конфигурация (application.yml)

```yaml
server:
  port: 8080

masker:
  grpc:
    host: localhost
    port: 50051
    timeout-ms: 500
  correlation:
    ttl-minutes: 10
  config:
    systems: classpath:config/systems.yaml
    priorities: classpath:config/priorities.yaml
    combinations: classpath:config/combinations.yaml
    reload-seconds: 30
```