package ru.cs.pers_data_masker.detector;

import org.junit.jupiter.api.Test;
import ru.cs.pers_data_masker.domain.Span;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PassportIssuerDetectorTest {

    private final PassportIssuerDetector detector = new PassportIssuerDetector();

    @Test
    void detectsIssuerWithCityAbbreviation() {
        assertDetected("паспорт выдан УФМС России по г. Москва", "УФМС России по г. Москва");
    }

    @Test
    void detectsIssuerPlain() {
        assertDetected("паспорт выдан Отделом УФМС России", "Отделом УФМС России");
    }

    @Test
    void stopsAtSentenceEnd() {
        assertDetected("паспорт выдан УФМС России.", "УФМС России");
    }

    private void assertDetected(String text, String expected) {
        List<Span> spans = detector.detect(text);
        assertThat(spans).isNotEmpty();
        assertThat(spans.get(0).original()).isEqualTo(expected);
        assertThat(spans.get(0).type()).isEqualTo("PASSPORT_ISSUER");
    }
}