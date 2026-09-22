package ru.cs.pers_data_masker.masking;

/**
 * Единая точка резолва «имя типа ПД → стратегия маскирования».
 *
 * <p>Объединяет встроенные типы (enum {@code PiiType}) и кастомные типы из YAML.
 * Реализация — на этапе 6.5.
 */
public interface PiiTypeResolver {

    /**
     * Возвращает стратегию маскирования для типа ПД.
     *
     * @param type имя типа ПД
     * @return стратегия маскирования
     */
    MaskingStrategy strategyFor(String type);
}