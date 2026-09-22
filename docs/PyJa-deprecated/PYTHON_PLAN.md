# План реализации Python-приложения (FastAPI + gRPC + Natasha)

## 1. Структура проекта

```
python-app/
├── requirements.txt
├── pyproject.toml
├── app/
│   ├── __init__.py
│   ├── main.py                    # gRPC-сервер
│   ├── config.py                  # загрузка конфигурации
│   ├── proto/
│   │   ├── detect.proto
│   │   ├── detect_pb2.py          # сгенерировано
│   │   └── detect_pb2_grpc.py     # сгенерировано
│   ├── detectors/
│   │   ├── __init__.py
│   │   ├── base.py                # интерфейс Detector
│   │   ├── registry.py            # реестр детекторов
│   │   ├── regex_detector.py      # детектор из YAML-правил
│   │   ├── natasha_detector.py    # Natasha NER (ФИО, даты, адреса)
│   │   ├── context.py             # контекстные маркеры + confidence
│   │   └── normalizer.py          # нормализация (нижний регистр)
│   ├── models.py                  # Span, SpanType
│   └── service.py                 # DetectService (сборка спанов)
├── config/
│   └── detectors.yaml             # regex-правила
└── tests/
    └── test_detectors.py
```

## 2. Порядок реализации

### Этап 1: Каркас и зависимости
- [ ] `requirements.txt`: grpcio, grpcio-tools, natasha, pyyaml
- [ ] `config.py` — загрузка `detectors.yaml`
- [ ] `models.py` — `Span(start, end, type, confidence)`

### Этап 2: Proto-контракт
- [ ] `detect.proto` — `DetectRequest { text }`, `DetectResponse { spans }`
- [ ] Сгенерировать `detect_pb2.py`, `detect_pb2_grpc.py` (grpcio-tools)

### Этап 3: Детекторы
- [ ] `base.py` — интерфейс `Detector.detect(text) -> List[Span]`
- [ ] `regex_detector.py` — строит детекторы из YAML-правил (паспорт, ИНН, email, телефон, карта, CVV, PIN, разделяющие слова)
- [ ] `natasha_detector.py` — Natasha NER (ФИО, даты, адреса, места рождения)
- [ ] `normalizer.py` — нормализация текста (нижний регистр), позиции по исходному тексту
- [ ] `context.py` — контекстные маркеры («клиент», «родился», «выдан», «пин») + расчёт confidence

### Этап 4: Реестр и сервис
- [ ] `registry.py` — регистрация всех детекторов
- [ ] `service.py` — `DetectService.detect(text)`: нормализация → все детекторы → объединение спанов → контекст/confidence

### Этап 5: gRPC-сервер
- [ ] `main.py` — gRPC-сервер, реализация `DetectService`
- [ ] Обработка больших текстов (до 100k токенов) одним запросом
- [ ] Таймауты и обработка ошибок

### Этап 6: Тесты
- [ ] `test_detectors.py` — юнит-тесты детекторов на примерах из ТЗ

## 3. Proto-контракт (detect.proto)

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

## 4. Конфигурация (detectors.yaml)

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

## 5. Логика детекции

```
Detect(text):
  → нормализовать текст (нижний регистр), сохранить маппинг позиций
  → regex-детекторы (из YAML) → спаны
  → Natasha NER → спаны (ФИО, даты, адреса)
  → контекстные маркеры → уточнить confidence
  → объединить все спаны
  → вернуть List[Span]
```

## 6. Зависимости (requirements.txt)

```
grpcio
grpcio-tools
natasha
pyyaml
```

## 7. Запуск

```bash
pip install -r requirements.txt
python -m grpc_tools.protoc -I app/proto --python_out=app/proto --grpc_python_out=app/proto app/proto/detect.proto
python -m app.main
```