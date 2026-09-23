package ru.cs.pers_data_masker.detector;

import org.junit.jupiter.api.Test;
import ru.cs.pers_data_masker.domain.PersonProfile;
import ru.cs.pers_data_masker.domain.Span;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PersonProfileBuilderTest {

    private final PersonProfileBuilder builder = new PersonProfileBuilder();

    @Test
    void groupsNearbySpansIntoOneProfile() {
        Span name = new Span(0, 10, "FULL_NAME", "Иванов Иван", 0.9);
        Span birth = new Span(20, 30, "BIRTH_DATE", "15.03.1990", 0.9);
        List<PersonProfile> profiles = builder.build(List.of(name, birth));
        assertThat(profiles).hasSize(1);
        assertThat(profiles.get(0).size()).isEqualTo(2);
    }

    @Test
    void separatesDistantSpans() {
        Span a = new Span(0, 5, "A", "12345", 0.9);
        Span b = new Span(500, 505, "B", "67890", 0.9);
        List<PersonProfile> profiles = builder.build(List.of(a, b));
        assertThat(profiles).hasSize(2);
    }

    @Test
    void boostsConfidenceForGroupedSpans() {
        Span name = new Span(0, 10, "FULL_NAME", "Иванов Иван", 0.6);
        Span birth = new Span(20, 30, "BIRTH_DATE", "15.03.1990", 0.6);
        List<PersonProfile> profiles = builder.build(List.of(name, birth));
        assertThat(profiles.get(0).spans())
                .allMatch(s -> s.confidence() == 1.0);
    }

    @Test
    void keepsConfidenceForSingleSpan() {
        Span name = new Span(0, 10, "FULL_NAME", "Иванов Иван", 0.6);
        List<PersonProfile> profiles = builder.build(List.of(name));
        assertThat(profiles.get(0).spans().get(0).confidence()).isEqualTo(0.6);
    }
}