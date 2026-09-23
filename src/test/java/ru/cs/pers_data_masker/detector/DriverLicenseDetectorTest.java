package ru.cs.pers_data_masker.detector;

import org.junit.jupiter.api.Test;
import ru.cs.pers_data_masker.domain.Span;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DriverLicenseDetectorTest {

    private final DriverLicenseDetector detector = new DriverLicenseDetector();

    @Test
    void detectsStandardFormat() {
        assertDetected("Водительское удостоверение 7712 345678", "7712 345678");
    }

    @Test
    void detectsTwoTwoSixFormat() {
        assertDetected("В/у 77 12 345678", "77 12 345678");
    }

    @Test
    void ignoresWithoutContext() {
        assertThat(detector.detect("Номер 7712 345678")).isEmpty();
    }

    private void assertDetected(String text, String expected) {
        List<Span> spans = detector.detect(text);
        assertThat(spans).isNotEmpty();
        assertThat(spans.get(0).original()).isEqualTo(expected);
        assertThat(spans.get(0).type()).isEqualTo("DRIVER_LICENSE");
    }
}