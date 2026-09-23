package ru.cs.pers_data_masker.detector;

import org.junit.jupiter.api.Test;
import ru.cs.pers_data_masker.domain.Span;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InnDetectorTest {

    private final InnDetector detector = new InnDetector();

    @Test
    void detectsValid12DigitInnWithContext() {
        assertDetected("ИНН 500100732259", "500100732259");
    }

    @Test
    void detectsValid12DigitInnCaseInsensitive() {
        assertDetected("инн 500100732259", "500100732259");
        assertDetected("Inn 500100732259", "500100732259");
        assertDetected("ИНН физического лица 500100732259", "500100732259");
    }

    @Test
    void detectsValid12DigitInnWithFullDecoding() {
        assertDetected("идентификационный номер налогоплательщика 500100732259",
                "500100732259");
    }

    @Test
    void ignores10DigitLegalEntityInn() {
        assertThat(detector.detect("ИНН 7707083893")).isEmpty();
    }

    @Test
    void invalidChecksumWithoutContextHasLowConfidence() {
        List<Span> spans = detector.detect("номер 123456789012");
        assertThat(spans).isNotEmpty();
        assertThat(spans.get(0).confidence()).isEqualTo(0.4);
    }

    @Test
    void detectsInvalidChecksumWithContext() {
        List<Span> spans = detector.detect("ИНН 123456789012");
        assertThat(spans).isNotEmpty();
        assertThat(spans.get(0).confidence()).isEqualTo(0.6);
    }

    @Test
    void confidenceIsHighWithContext() {
        List<Span> spans = detector.detect("ИНН 500100732259");
        assertThat(spans).isNotEmpty();
        assertThat(spans.get(0).confidence()).isEqualTo(0.9);
    }

    @Test
    void confidenceIsMediumWithoutContext() {
        List<Span> spans = detector.detect("номер 500100732259");
        assertThat(spans).isNotEmpty();
        assertThat(spans.get(0).confidence()).isEqualTo(0.6);
    }

    private void assertDetected(String text, String expected) {
        List<Span> spans = detector.detect(text);
        assertThat(spans).isNotEmpty();
        assertThat(spans.get(0).original()).isEqualTo(expected);
        assertThat(spans.get(0).type()).isEqualTo("INN");
    }
}