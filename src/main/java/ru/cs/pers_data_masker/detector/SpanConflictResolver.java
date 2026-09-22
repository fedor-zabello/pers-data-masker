package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.Span;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Разрешает пересечения спанов от разных детекторов.
 *
 * <p>Правила (детерминированные):
 * <ol>
 *   <li>Приоритет по типу ПД (больше = важнее).</li>
 *   <li>При равном приоритете — «самый длинный/специфичный выигрывает».</li>
 *   <li>Проигравшие спаны отбрасываются.</li>
 * </ol>
 */
@Component
public class SpanConflictResolver {

    /**
     * Разрешает пересечения, возвращая непересекающийся набор спанов.
     *
     * @param spans            исходные спаны (могут пересекаться)
     * @param priorityResolver функция «тип ПД → приоритет»
     * @return непересекающиеся спаны
     */
    public List<Span> resolve(List<Span> spans, ToIntFunction<String> priorityResolver) {
        if (spans == null || spans.isEmpty()) {
            return List.of();
        }
        List<Span> sorted = new ArrayList<>(spans);
        sorted.sort(Comparator
                .comparingInt(Span::start)
                .thenComparing(Comparator.comparingInt((Span s) -> priorityResolver.applyAsInt(s.type())).reversed())
                .thenComparing(Comparator.comparingInt(Span::length).reversed()));

        List<Span> result = new ArrayList<>();
        for (Span span : sorted) {
            if (result.isEmpty() || span.start() >= result.get(result.size() - 1).end()) {
                result.add(span);
            } else {
                Span last = result.get(result.size() - 1);
                if (wins(span, last, priorityResolver)) {
                    result.set(result.size() - 1, span);
                }
            }
        }
        return result;
    }

    private static boolean wins(Span candidate, Span current, ToIntFunction<String> priorityResolver) {
        int candidatePriority = priorityResolver.applyAsInt(candidate.type());
        int currentPriority = priorityResolver.applyAsInt(current.type());
        if (candidatePriority != currentPriority) {
            return candidatePriority > currentPriority;
        }
        return candidate.length() > current.length();
    }
}