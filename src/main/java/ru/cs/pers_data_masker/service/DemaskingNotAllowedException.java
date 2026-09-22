package ru.cs.pers_data_masker.service;

/**
 * Исключение: системе не разрешено демаскирование, но запрос требует его.
 */
public class DemaskingNotAllowedException extends RuntimeException {

    public DemaskingNotAllowedException(String systemId) {
        super("Demasking is not allowed for system: " + systemId);
    }
}