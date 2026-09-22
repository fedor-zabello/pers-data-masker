package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Детектор ФИО / имени держателя карты.
 *
 * <p>Ловит последовательность из 2–3 слов с заглавной буквы (Фамилия Имя
 * Отчество). Словарь исключений отсекает упоминания известных лиц (поэт
 * Пушкин, Толстой и т.п.), которые не являются ПД.
 */
@Component
public class FullNameDetector extends AbstractRegexDetector {

    private static final String NAME_REGEX =
            "\\b[А-ЯЁ][а-яё]+(?:\\s+[А-ЯЁ][а-яё]+){1,2}\\b";

    private static final Set<String> EXCLUSIONS = Set.of(
            "Пушкин", "Толстой", "Достоевский", "Чехов", "Гоголь", "Лермонтов",
            "Есенин", "Маяковский", "Блок", "Тургенев", "Гончаров", "Островский",
            "Некрасов", "Фет", "Тютчев", "Бунин", "Куприн", "Горький", "Шолохов",
            "Булгаков", "Пастернак", "Ахматова", "Цветаева", "Гумилёв", "Мандельштам"
    );

    public FullNameDetector() {
        super(NAME_REGEX, PiiType.FULL_NAME.name(), 40);
    }

    @Override
    protected boolean accept(String text, int start, int end, String original) {
        String firstWord = original.split("\\s+")[0];
        if (firstWord.isEmpty() || !Character.isUpperCase(firstWord.charAt(0))) {
            return false;
        }
        return !EXCLUSIONS.contains(firstWord);
    }
}