package ru.cs.pers_data_masker.api;

/**
 * Единый формат ошибки.
 *
 * @param status  HTTP-статус
 * @param code    машинный код ошибки
 * @param message человекочитаемое сообщение
 */
public record ApiError(int status, String code, String message) {
}