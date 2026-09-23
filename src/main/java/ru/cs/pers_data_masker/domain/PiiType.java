package ru.cs.pers_data_masker.domain;

/**
 * Встроенные типы персональных данных (ПД).
 *
 * <p>Кастомные типы из YAML не являются элементами этого enum (enum фиксирован
 * на этапе компиляции). Для них используется строковое имя типа, которое
 * хранится в {@link Span} / {@link MaskFragment} наравне с именами встроенных
 * типов (см. {@code PiiTypeResolver}).
 */
public enum PiiType {

    FULL_NAME("ФИО"),
    BIRTH_DATE("Дата рождения"),
    BIRTH_PLACE("Место рождения"),
    PASSPORT("Серия и номер паспорта"),
    CITIZENSHIP("Гражданство"),
    PASSPORT_ISSUER("Орган, выдавший паспорт"),
    PASSPORT_DEPARTMENT_CODE("Код подразделения"),
    PASSPORT_ISSUE_DATE("Дата выдачи паспорта"),
    DRIVER_LICENSE("Серия и номер в/у"),
    ADDRESS("Адрес"),
    REGISTRATION_ADDRESS("Адрес регистрации"),
    RESIDENCE_ADDRESS("Адрес проживания"),
    EMAIL("Email"),
    PHONE("Номер телефона"),
    INN("ИНН"),
    CARD_NUMBER("Номер платёжной карты"),
    CVV("CVV"),
    PIN("Пин-код"),
    CARD_HOLDER("Имя держателя карты");

    private final String displayName;

    PiiType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}