package ru.cs.pers_data_masker.store;

import ru.cs.pers_data_masker.domain.MaskFragment;

import java.util.List;

/**
 * Хранилище соответствий «маска → оригинал» по {@code payload_id}.
 *
 * <p>Хранятся {@link MaskFragment}-ы (позиции в маске + оригинальные фрагменты),
 * а не весь оригинал и не {@code Span} — экономия памяти на текстах до 100k
 * токенов, и именно координаты в маске нужны для демаскирования.
 *
 * <p>Реализация по умолчанию — in-memory (Caffeine с TTL и ограничением размера).
 * Подменяема на Redis/stateless без изменения бизнес-логики.
 */
public interface CorrelationStore {

    /**
     * Сохраняет фрагменты соответствия по {@code payload_id}.
     *
     * @param payloadId идентификатор пары маскирование→демаскирование
     * @param fragments фрагменты соответствия
     */
    void put(String payloadId, List<MaskFragment> fragments);

    /**
     * Возвращает фрагменты соответствия по {@code payload_id}.
     *
     * @param payloadId идентификатор пары
     * @return фрагменты или {@code null}, если записи нет
     */
    List<MaskFragment> get(String payloadId);

    /**
     * Удаляет запись по {@code payload_id}.
     *
     * @param payloadId идентификатор пары
     */
    void remove(String payloadId);
}