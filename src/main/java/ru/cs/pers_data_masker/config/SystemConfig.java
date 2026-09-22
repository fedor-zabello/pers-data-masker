package ru.cs.pers_data_masker.config;

import java.util.List;

/**
 * Профиль системы-потребителя.
 *
 * @param enabled    включён ли доступ для системы
 * @param demasking  разрешено ли демаскирование
 * @param types      перечень типов ПД для идентификации/маскирования
 */
public record SystemConfig(
        boolean enabled,
        boolean demasking,
        List<String> types
) {

    public static SystemConfig defaults() {
        return new SystemConfig(true, true, List.of());
    }
}