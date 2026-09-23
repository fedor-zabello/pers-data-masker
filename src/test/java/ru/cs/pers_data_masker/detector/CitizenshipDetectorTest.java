package ru.cs.pers_data_masker.detector;

import org.junit.jupiter.api.Test;
import ru.cs.pers_data_masker.domain.Span;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CitizenshipDetectorTest {

    private final CitizenshipDetector detector = new CitizenshipDetector();

    @Test
    void detectsCitizenship() {
        assertDetected("Гражданин Российской Федерации", "Гражданин Российской Федерации");
    }

    @Test
    void detectsCitizenshipShort() {
        assertDetected("Гражданство РФ", "Гражданство РФ");
    }

    @Test
    void ignoresSurnameAfterCitizen() {
        assertThat(detector.detect("Гражданин Петров")).isEmpty();
    }

    private void assertDetected(String text, String expected) {
        List<Span> spans = detector.detect(text);
        assertThat(spans).isNotEmpty();
        assertThat(spans.get(0).original()).isEqualTo(expected);
        assertThat(spans.get(0).type()).isEqualTo("CITIZENSHIP");
    }
}