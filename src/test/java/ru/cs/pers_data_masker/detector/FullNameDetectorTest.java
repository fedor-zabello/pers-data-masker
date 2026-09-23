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
    void detectsNameWithAbbreviation() {
        assertDetected("Гражданин РФ Сидоров Алексей.", "РФ Сидоров Алексей");
    }

    @Test
    void detectsSurnameWithInitials() {
        assertDetected("Иванов И. И.", "Иванов И. И.");
    }

    @Test
    void detectsSurnameWithInitialsNoSpaces() {
        assertDetected("Иванов И.И.", "Иванов И.И.");
    }

    @Test
    void detectsInitialsThenSurname() {
        assertDetected("И. И. Иванов", "И. И. Иванов");
    }

    @Test
    void detectsHyphenatedSurname() {
        assertDetected("Петров-Водкин Иван", "Петров-Водкин Иван");
    }

    @Test
    void ignoresKnownPerson() {
        assertThat(detector.detect("Поэт Александр Пушкин написал стихи.")).isEmpty();
    }

    @Test
    void detectsNameAfterRoleWord() {
        assertDetected("Клиент Иванов Иван", "Иванов Иван");
    }

    private void assertDetected(String text, String expected) {
        List<Span> spans = detector.detect(text);
        assertThat(spans).isNotEmpty();
        assertThat(spans.get(0).original()).isEqualTo(expected);
        assertThat(spans.get(0).type()).isEqualTo("FULL_NAME");
    }
}