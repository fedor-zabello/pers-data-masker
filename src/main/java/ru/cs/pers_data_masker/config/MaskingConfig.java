package ru.cs.pers_data_masker.config;

/**
 * Конфигурация правила маскирования для кастомного типа ПД.
 *
 * @param keepStart           сколько символов оставить в начале фрагмента
 * @param keepEnd             сколько символов оставить в конце фрагмента
 * @param maskChar            символ маскирования
 * @param preserveSeparators  сохранять не-буквенно-цифровые символы (разделители)
 */
public record MaskingConfig(
        int keepStart,
        int keepEnd,
        String maskChar,
        boolean preserveSeparators
) {

    public static MaskingConfig defaults() {
        return new MaskingConfig(0, 0, "*", false);
    }
}