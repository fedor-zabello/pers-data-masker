package ru.cs.pers_data_masker.detector;

import org.junit.jupiter.api.Test;
import ru.cs.pers_data_masker.domain.Span;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PassportSeparatedDetectorTest {

    private final PassportSeparatedDetector detector = new PassportSeparatedDetector();

    @Test
    void detectsSeparatedPassport() {
        assertDetected("серия 4509 номер 123456", "серия 4509 номер 123456");
    }

    @Test
    void detectsSeparatedPassportWithWord() {
        assertDetected("паспорт серия 4509 номер 123456", "паспорт серия 4509 номер 123456");
    }

    private void assertDetected(String text, String expected) {
        List<Span> spans = detector.detect(text);
        assertThat(spans).isNotEmpty();
        assertThat(spans.get(0).original()).isEqualTo(expected);
        assertThat(spans.get(0).type()).isEqualTo("PASSPORT");
    }
}