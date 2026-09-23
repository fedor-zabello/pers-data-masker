package ru.cs.pers_data_masker.detector;

import org.junit.jupiter.api.Test;
import ru.cs.pers_data_masker.domain.Span;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RegexPiiDetectorTest {

    @Test
    void detectsWithoutContextRequirement() {
        RegexPiiDetector detector = new RegexPiiDetector("\\b\\d{4}\\b", "CODE", 50);
        List<Span> spans = detector.detect("Код 1234");
        assertThat(spans).hasSize(1);
        assertThat(spans.get(0).type()).isEqualTo("CODE");
        assertThat(spans.get(0).confidence()).isEqualTo(1.0);
    }

    @Test
    void requiresContextWhenConfigured() {
        RegexPiiDetector detector = new RegexPiiDetector(
                "\\b\\d{4}\\b", "CODE", 50, List.of("договор"), true, 40);
        assertThat(detector.detect("Номер договора 1234")).hasSize(1);
        assertThat(detector.detect("Просто число 1234")).isEmpty();
    }

    @Test
    void checksumRejectsInvalid() {
        RegexPiiDetector detector = new RegexPiiDetector(
                "\\b\\d{13,19}\\b", "CARD", 50, List.of(), false, 40, "luhn");
        assertThat(detector.detect("4111111111111111")).hasSize(1);
        assertThat(detector.detect("1234567890123456")).isEmpty();
    }

    @Test
    void checksumWithContext() {
        RegexPiiDetector detector = new RegexPiiDetector(
                "\\b\\d{13,19}\\b", "CARD", 50, List.of("карта"), true, 40, "luhn");
        assertThat(detector.detect("Карта 4111111111111111")).hasSize(1);
        assertThat(detector.detect("4111111111111111")).isEmpty();
    }
}