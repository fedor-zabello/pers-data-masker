package ru.cs.pers_data_masker.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChecksumRegistryTest {

    @Test
    void luhnValid() {
        assertThat(ChecksumRegistry.luhn("4111111111111111")).isTrue();
    }

    @Test
    void luhnInvalid() {
        assertThat(ChecksumRegistry.luhn("1234567890123456")).isFalse();
    }

    @Test
    void innValid10() {
        assertThat(ChecksumRegistry.inn("7707083893")).isTrue();
    }

    @Test
    void innValid12() {
        assertThat(ChecksumRegistry.inn("500100732259")).isTrue();
    }

    @Test
    void innInvalid() {
        assertThat(ChecksumRegistry.inn("1234567890")).isFalse();
    }

    @Test
    void snilsValid() {
        assertThat(ChecksumRegistry.snils("112-233-445 95")).isTrue();
    }

    @Test
    void snilsInvalid() {
        assertThat(ChecksumRegistry.snils("123-456-789 01")).isFalse();
    }

    @Test
    void forNameUnknownReturnsNull() {
        assertThat(ChecksumRegistry.forName("unknown")).isNull();
    }
}