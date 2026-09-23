package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PersonProfile;
import ru.cs.pers_data_masker.domain.Span;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Группирует спаны ПД в профили людей по близости в тексте.
 *
 * <p>Спаны, расположенные в пределах {@code GROUP_WINDOW} символов друг от друга,
 * считаются относящимися к одному человеку. Если в группе несколько полей,
 * уверенность каждого спана повышается до {@code BOOSTED_CONFIDENCE} — наличие
 * нескольких полей одного человека усиливает вероятность того, что это ПД.
 */
@Component
public class PersonProfileBuilder {

    /** Максимальное расстояние между спанами одной группы (символов). */
    private static final int GROUP_WINDOW = 200;

    /** Уверенность спана, если он входит в группу из нескольких полей. */
    private static final double BOOSTED_CONFIDENCE = 1.0;

    /**
     * Группирует спаны в профили и повышает уверенность связанных полей.
     *
     * @param spans спаны (уже разрешённые от пересечений)
     * @return список профилей
     */
    public List<PersonProfile> build(List<Span> spans) {
        if (spans == null || spans.isEmpty()) {
            return List.of();
        }
        List<Span> sorted = new ArrayList<>(spans);
        sorted.sort(Comparator.comparingInt(Span::start));

        List<PersonProfile> profiles = new ArrayList<>();
        PersonProfile current = null;
        int lastEnd = -1;

        for (Span span : sorted) {
            if (current == null || span.start() - lastEnd > GROUP_WINDOW) {
                current = new PersonProfile();
                profiles.add(current);
            }
            current.add(span);
            lastEnd = Math.max(lastEnd, span.end());
        }

        for (PersonProfile profile : profiles) {
            if (profile.size() >= 2) {
                boost(profile);
            }
        }
        return profiles;
    }

    private void boost(PersonProfile profile) {
        List<Span> spans = profile.spans();
        for (int i = 0; i < spans.size(); i++) {
            Span s = spans.get(i);
            profile.replace(i, new Span(s.start(), s.end(), s.type(), s.original(), BOOSTED_CONFIDENCE));
        }
    }
}