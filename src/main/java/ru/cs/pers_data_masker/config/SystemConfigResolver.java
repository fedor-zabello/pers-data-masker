package ru.cs.pers_data_masker.config;

import org.springframework.stereotype.Component;

/**
 * Резолвит профиль системы по заголовку.
 *
 * <p>Обязательный {@code default}-профиль применяется, если заголовок отсутствует
 * или система не найдена. Если {@code default} не задан в конфигурации —
 * используется встроенный дефолтный профиль (включён, демаскирование разрешено,
 * все типы ПД).
 */
@Component
public class SystemConfigResolver {

    public static final String DEFAULT_SYSTEM = "default";

    private final PiiProperties properties;

    public SystemConfigResolver(PiiProperties properties) {
        this.properties = properties;
    }

    /**
     * Возвращает профиль системы по идентификатору.
     *
     * @param systemId идентификатор системы (может быть {@code null})
     * @return профиль системы (никогда не {@code null})
     */
    public SystemConfig resolve(String systemId) {
        if (systemId != null && properties.systems().containsKey(systemId)) {
            return properties.systems().get(systemId);
        }
        if (properties.systems().containsKey(DEFAULT_SYSTEM)) {
            return properties.systems().get(DEFAULT_SYSTEM);
        }
        return SystemConfig.defaults();
    }

    /**
     * Проверяет, включена ли система.
     *
     * @param systemId идентификатор системы
     * @return {@code true}, если система включена
     */
    public boolean isEnabled(String systemId) {
        return resolve(systemId).enabled();
    }
}