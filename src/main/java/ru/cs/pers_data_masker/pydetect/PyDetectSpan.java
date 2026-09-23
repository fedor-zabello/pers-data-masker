package ru.cs.pers_data_masker.pydetect;

/**
 * Спан, возвращаемый Python-микросервисом детекции по HTTP.
 *
 * @param start      начальная позиция в исходном тексте (включительно)
 * @param end        конечная позиция в исходном тексте (исключительно)
 * @param type       тип ПД (имя из Python)
 * @param confidence уверенность детекции (0..1)
 */
public record PyDetectSpan(int start, int end, String type, float confidence) {
}