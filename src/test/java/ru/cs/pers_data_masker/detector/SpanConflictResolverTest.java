package ru.cs.pers_data_masker.detector;

import org.junit.jupiter.api.Test;
import ru.cs.pers_data_masker.domain.Span;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SpanConflictResolverTest {

    private final SpanConflictResolver resolver = new SpanConflictResolver();
    private final Map<String, Integer> priorities = Map.of(
            "LOW", 10,
            "HIGH", 50
    );

    @Test
    void higherPriorityWinsOverlap() {
        List<Span> spans = List.of(
                new Span(0, 10, "LOW", "lowvalue"),
                new Span(2, 8, "HIGH", "high")
        );
        List<Span> result = resolver.resolve(spans, priorities::get);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo("HIGH");
    }

    @Test
    void longerWinsWhenEqualPriority() {
        List<Span> spans = List.of(
                new Span(0, 10, "LOW", "0123456789"),
                new Span(2, 6, "LOW", "2345")
        );
        List<Span> result = resolver.resolve(spans, priorities::get);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).length()).isEqualTo(10);
    }

    @Test
    void nonOverlappingSpansAllKept() {
        List<Span> spans = List.of(
                new Span(0, 3, "LOW", "abc"),
                new Span(5, 8, "HIGH", "def")
        );
        List<Span> result = resolver.resolve(spans, priorities::get);
        assertThat(result).hasSize(2);
    }
}