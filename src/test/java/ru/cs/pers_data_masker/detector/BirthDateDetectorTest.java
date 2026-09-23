package ru.cs.pers_data_masker.detector;

import org.junit.jupiter.api.Test;
import ru.cs.pers_data_masker.domain.Span;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BirthDateDetectorTest {

    private final BirthDateDetector detector = new BirthDateDetector();

    @Test
    void detectsDayMonthYear() {
        assertDetected("Дата рождения клиента: 15.03.1990.", "15.03.1990");
    }

    @Test
    void detectsMonthDayYear() {
        assertDetected("Дата рождения: 03.15.1990.", "03.15.1990");
    }

    @Test
    void detectsYearDayMonth() {
        assertDetected("Дата рождения: 1990.15.03.", "1990.15.03");
    }

    @Test
    void detectsDashSeparated() {
        assertDetected("Дата рождения: 15-03-1990.", "15-03-1990");
    }

    @Test
    void detectsSlashSeparated() {
        assertDetected("Дата рождения: 15/03/1990.", "15/03/1990");
    }

    @Test
    void detectsNoSeparators() {
        assertDetected("Дата рождения: 15031990.", "15031990");
    }

    @Test
    void detectsMonthAsWord() {
        assertDetected("Дата рождения: 15 марта 1990 года.", "15 марта 1990");
    }

    @Test
    void detectsInFullNameContext() {
        assertDetected("Иванов Иван Иванович, 15.03.1990 года рождения.", "15.03.1990");
    }

    @Test
    void ignoresDateWithoutBirthContext() {
        List<Span> spans = detector.detect("Срок действия договора до 15.03.1990.");
        assertThat(spans).isEmpty();
    }

    @Test
    void confidenceIsHighWithContext() {
        List<Span> spans = detector.detect("Дата рождения: 15.03.1990.");
        assertThat(spans).hasSize(1);
        assertThat(spans.get(0).confidence()).isEqualTo(0.9);
    }

    private void assertDetected(String text, String expected) {
        List<Span> spans = detector.detect(text);
        assertThat(spans).hasSize(1);
        assertThat(spans.get(0).original()).isEqualTo(expected);
        assertThat(spans.get(0).type()).isEqualTo("BIRTH_DATE");
    }
}