package ru.cs.pers_data_masker.api;

/**
 * Ответ {@code POST /process}.
 *
 * @param result результат обработки (маска или исходная строка)
 */
public record ProcessResponse(String result) {
}