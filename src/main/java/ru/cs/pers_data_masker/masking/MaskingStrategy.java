package ru.cs.pers_data_masker.masking;

/**
 * Стратегия маскирования фрагмента ПД.
 *
 * <p>Задел под токенизацию/синтетику и разный вид маски по системе. Каждый тип
 * ПД может иметь свою стратегию.
 */
public interface MaskingStrategy {

    /**
     * Маскирует фрагмент ПД.
     *
     * @param type     имя типа ПД
     * @param original исходный фрагмент
     * @return замаскированный фрагмент
     */
    String mask(String type, String original);
}