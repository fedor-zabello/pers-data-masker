package ru.cs.pers_data_masker.masking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultMaskingStrategyTest {

    private final DefaultMaskingStrategy strategy = new DefaultMaskingStrategy();

    @Test
    void masksPhoneWithPrefix() {
        assertThat(strategy.mask("PHONE", "+7 (900) 123-45-67")).isEqualTo("+7 (9**) ***-**-**");
    }

    @Test
    void masksPhoneWithoutPrefix() {
        assertThat(strategy.mask("PHONE", "900-123-45-67")).isEqualTo("9**-***-**-**");
    }

    @Test
    void masksFullName() {
        assertThat(strategy.mask("FULL_NAME", "Иванов Иван Иванович")).isEqualTo("И. И. И.");
    }

    @Test
    void masksCardNumber() {
        assertThat(strategy.mask("CARD_NUMBER", "1234 5678 9012 3456")).isEqualTo("1234 **** **** 3456");
    }

    @Test
    void masksEmail() {
        assertThat(strategy.mask("EMAIL", "ivan@mail.ru")).isEqualTo("i***@mail.ru");
    }

    @Test
    void masksMilitaryId() {
        assertThat(strategy.mask("MILITARY_ID", "АБ 3456789")).isEqualTo("АБ *******");
    }

    @Test
    void masksSeamanPassport() {
        assertThat(strategy.mask("SEAMAN_PASSPORT", "АБ 3456789")).isEqualTo("АБ *******");
    }
}