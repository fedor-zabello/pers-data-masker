package ru.cs.pers_data_masker.detector;

import org.junit.jupiter.api.Test;
import ru.cs.pers_data_masker.domain.Span;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationAddressDetectorTest {

    private final RegistrationAddressDetector detector = new RegistrationAddressDetector();

    @Test
    void detectsRegistrationAddress() {
        assertDetected("зарегистрирован по адресу: г. Москва, ул. Ленина, д. 10",
                "зарегистрирован по адресу: г. Москва, ул. Ленина, д. 10");
    }

    @Test
    void detectsRegistrationPlace() {
        assertDetected("Место регистрации: г. Казань, ул. Пушкина, д. 5",
                "Место регистрации: г. Казань, ул. Пушкина, д. 5");
    }

    private void assertDetected(String text, String expected) {
        List<Span> spans = detector.detect(text);
        assertThat(spans).isNotEmpty();
        assertThat(spans.get(0).original()).isEqualTo(expected);
        assertThat(spans.get(0).type()).isEqualTo("REGISTRATION_ADDRESS");
    }
}