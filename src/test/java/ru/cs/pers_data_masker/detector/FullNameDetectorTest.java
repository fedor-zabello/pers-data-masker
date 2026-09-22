package ru.cs.pers_data_masker.detector;

import org.junit.jupiter.api.Test;
import ru.cs.pers_data_masker.domain.Span;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FullNameDetectorTest {

    private final FullNameDetector detector = new FullNameDetector();

    @Test
    void detectsFullName() {
        assertDetected("Иванов Иван Иванович", "Иванов Иван Иванович");
    }

    @Test
    void detectsNameWithoutPatronymic() {
        assertDetected("Сидоров Алексей", "Сидоров Алексей");
    }

    @Test
    void detectsNameInSentence() {
        assertDetected("Клиент Иванов Иван Иванович обратился в отделение банка.",
                "Иванов Иван Иванович");
    }

    @Test
    void detectsNameAfterAbbreviation() {
        assertDetected("Гражданин РФ Сидоров Алексей.", "Сидоров Алексей");
    }

    @Test
    void ignoresCommonPhrase() {
        assertThat(detector.detect("Дата рождения клиента: 15.03.1990.")).isEmpty();
    }

    @Test
    void ignoresPassportPhrase() {
        assertThat(detector.detect("Паспорт серия 4509 123456 выдан ОВД района.")).isEmpty();
    }

    @Test
    void ignoresKnownPerson() {
        assertThat(detector.detect("Поэт Александр Пушкин написал стихи.")).isEmpty();
    }

    private void assertDetected(String text, String expected) {
        List<Span> spans = detector.detect(text);
        assertThat(spans).isNotEmpty();
        assertThat(spans.get(0).original()).isEqualTo(expected);
        assertThat(spans.get(0).type()).isEqualTo("FULL_NAME");
    }
}