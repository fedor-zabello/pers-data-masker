package ru.cs.pers_data_masker.detector;

import org.junit.jupiter.api.Test;
import ru.cs.pers_data_masker.domain.Span;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AddressDetectorTest {

    private final AddressDetector detector = new AddressDetector();

    @Test
    void detectsAddressByMarkers() {
        assertDetected("г. Москва, ул. Ленина, д. 10", "г. Москва, ул. Ленина, д. 10");
    }

    @Test
    void detectsAddressByTrigger() {
        assertDetected("проживает по адресу: Москва, Ленина 10", "Москва, Ленина 10");
    }

    @Test
    void detectsAddressByTriggerWithoutMarkers() {
        assertDetected("проживает Москва, Ленина 10", "Москва, Ленина 10");
    }

    @Test
    void ignoresBankAddress() {
        assertThat(detector.detect("Отделение банка: г. Москва, ул. Тверская, д. 1")).isEmpty();
    }

    private void assertDetected(String text, String expected) {
        List<Span> spans = detector.detect(text);
        assertThat(spans).isNotEmpty();
        assertThat(spans.get(0).original()).isEqualTo(expected);
        assertThat(spans.get(0).type()).isEqualTo("ADDRESS");
    }
}