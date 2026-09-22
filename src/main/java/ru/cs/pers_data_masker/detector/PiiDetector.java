package ru.cs.pers_data_masker.detector;

import ru.cs.pers_data_masker.domain.Span;

import java.util.List;

/**
 * Детектор персональных данных (ПД) одного типа.
 *
 * <p>Каждый тип ПД — отдельный {@code @Component}. Spring собирает все в
 * {@code List<PiiDetector>}. Новый тип ПД = новый класс (или запись в YAML для
 * кастомных типов), ядро не меняется.
 */
public interface PiiDetector {

    /**
     * Находит все вхождения ПД данного типа в тексте.
     *
     * @param text исходный текст
     * @return список спанов (может быть пустым)
     */
    List<Span> detect(String text);

    /**
     * Имя типа ПД (для встроенных — имя enum {@code PiiType}, для кастомных —
     * имя из YAML).
     */
    String type();

    /**
     * Приоритет типа при разрешении пересечений спанов (больше = важнее).
     */
    int priority();
}