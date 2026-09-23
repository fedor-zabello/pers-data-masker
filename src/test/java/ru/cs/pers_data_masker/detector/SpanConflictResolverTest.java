package ru.cs.pers_data_masker.detector;

import org.junit.jupiter.api.Test;
import ru.cs.pers_data_masker.domain.Span;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpanConflictResolverTest {

    private final SpanConflictResolver resolver = new SpanConflictResolver();

    @Test
    void higherPriorityWins() {
        Span low = new Span(0, 5, "LOW", "12345", 0.9);
        Span high = new Span(0, 5, "HIGH", "12345", 0.5);
        List<Span> result = resolver.resolve(List.of(low, high), t -> t.equals("HIGH") ? 100 : 10);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo("HIGH");
    }

    @Test
    void higherConfidenceWinsOnEqualPriority() {
        Span lowConf = new Span(0, 5, "A", "12345", 0.6);
        Span highConf = new Span(0, 5, "B", "12345", 0.9);
        List<Span> result = resolver.resolve(List.of(lowConf, highConf), t -> 50);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo("B");
    }

    @Test
    void longerWinsOnEqualPriorityAndConfidence() {
        Span shortSpan = new Span(0, 3, "A", "123", 0.9);
        Span longSpan = new Span(0, 5, "B", "12345", 0.9);
        List<Span> result = resolver.resolve(List.of(shortSpan, longSpan), t -> 50);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo("B");
    }

    @Test
    void nonOverlappingSpansKept() {
        Span a = new Span(0, 3, "A", "123", 0.9);
        Span b = new Span(5, 8, "B", "456", 0.9);
        List<Span> result = resolver.resolve(List.of(a, b), t -> 50);
        assertThat(result).hasSize(2);
    }
}