package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;
import ru.cs.pers_data_masker.domain.Span;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Детектор ФИО / имени держателя карты.
 *
 * <p>Ищет известные русские имена и отчества (словарь + суффиксы отчеств) и
 * расширяет найденное влево на предшествующие слова с заглавной буквы (фамилия).
 * Такой подход не ловит произвольные последовательности заглавных слов
 * («Дата рождения клиента», «Паспорт серия»). Словарь исключений отсекает
 * упоминания известных лиц (поэт Пушкин и т.п.).
 */
@Component
public class FullNameDetector extends AbstractRegexDetector {

    private static final String NAME_REGEX =
            "\\b[А-ЯЁ][а-яё]+(?:\\s+[А-ЯЁ][а-яё]+){1,2}\\b";

    private static final Set<String> FIRST_NAMES = Set.of(
            "Александр", "Алексей", "Анатолий", "Андрей", "Антон", "Артём", "Артур",
            "Борис", "Вадим", "Валентин", "Валерий", "Василий", "Виктор", "Виталий",
            "Владимир", "Владислав", "Вячеслав", "Геннадий", "Георгий", "Григорий",
            "Даниил", "Денис", "Дмитрий", "Евгений", "Егор", "Иван", "Игорь", "Илья",
            "Кирилл", "Константин", "Лев", "Леонид", "Максим", "Марк", "Матвей",
            "Михаил", "Никита", "Николай", "Олег", "Павел", "Пётр", "Роман", "Руслан",
            "Сергей", "Станислав", "Степан", "Тимофей", "Тимур", "Фёдор", "Юрий",
            "Анна", "Алина", "Алла", "Анастасия", "Валентина", "Валерия", "Вера",
            "Вероника", "Виктория", "Галина", "Дарья", "Диана", "Евгения", "Екатерина",
            "Елена", "Елизавета", "Жанна", "Зоя", "Ирина", "Ксения", "Лариса",
            "Лидия", "Любовь", "Людмила", "Марина", "Мария", "Надежда", "Наталья",
            "Нина", "Оксана", "Ольга", "Полина", "Раиса", "Светлана", "София",
            "Тамара", "Татьяна", "Юлия"
    );

    private static final Set<String> EXCLUSIONS = Set.of(
            "Пушкин", "Толстой", "Достоевский", "Чехов", "Гоголь", "Лермонтов",
            "Есенин", "Маяковский", "Блок", "Тургенев", "Гончаров", "Островский",
            "Некрасов", "Фет", "Тютчев", "Бунин", "Куприн", "Горький", "Шолохов",
            "Булгаков", "Пастернак", "Ахматова", "Цветаева", "Гумилёв", "Мандельштам"
    );

    private static final Set<String> NON_SURNAMES = Set.of(
            "Клиент", "Гражданин", "Гражданка", "Поэт", "Писатель", "Художник",
            "Директор", "Менеджер", "Сотрудник", "Специалист", "Инженер", "Врач",
            "Учитель", "Профессор", "Доктор", "Господин", "Госпожа", "Товарищ",
            "Дата", "Паспорт", "Серия", "Номер", "Код", "Мой", "Наш", "Ваш"
    );

    private static final Pattern CAPITALIZED_WORD = Pattern.compile("[А-ЯЁ][а-яё]+");

    public FullNameDetector() {
        super(NAME_REGEX, PiiType.FULL_NAME.name(), 40);
    }

    @Override
    public List<Span> detect(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        List<Span> result = new ArrayList<>();
        Matcher matcher = CAPITALIZED_WORD.matcher(text);
        while (matcher.find()) {
            String word = matcher.group();
            if (!isNameWord(word)) {
                continue;
            }
            int nameEnd = matcher.end();
            int maxWords = isPatronymic(word) ? 2 : 1;
            int nameStart = expandBackward(text, matcher.start(), maxWords);
            String original = text.substring(nameStart, nameEnd);
            if (isValidName(original)) {
                addNonOverlapping(result, new Span(nameStart, nameEnd, type(), original));
            }
        }
        return result;
    }

    private static void addNonOverlapping(List<Span> result, Span candidate) {
        for (int i = 0; i < result.size(); i++) {
            Span existing = result.get(i);
            if (candidate.start() < existing.end() && candidate.end() > existing.start()) {
                if (candidate.length() > existing.length()) {
                    result.set(i, candidate);
                }
                return;
            }
        }
        result.add(candidate);
    }

    private static boolean isNameWord(String word) {
        return FIRST_NAMES.contains(word) || isPatronymic(word);
    }

    private static int expandBackward(String text, int start, int maxWords) {
        int cursor = start;
        int words = 0;
        while (cursor > 0 && words < maxWords) {
            int prevEnd = cursor;
            while (prevEnd > 0 && Character.isWhitespace(text.charAt(prevEnd - 1))) {
                prevEnd--;
            }
            int prevStart = prevEnd;
            while (prevStart > 0 && !Character.isWhitespace(text.charAt(prevStart - 1))) {
                prevStart--;
            }
            String prevWord = text.substring(prevStart, prevEnd);
            if (prevWord.isEmpty() || !isCapitalizedWord(prevWord)) {
                break;
            }
            cursor = prevStart;
            words++;
        }
        return cursor;
    }

    private static boolean isCapitalizedWord(String word) {
        if (word.length() < 2) {
            return false;
        }
        if (!Character.isUpperCase(word.charAt(0))) {
            return false;
        }
        for (int i = 1; i < word.length(); i++) {
            if (!Character.isLowerCase(word.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidName(String original) {
        String[] words = original.trim().split("\\s+");
        if (words.length < 2 || words.length > 3) {
            return false;
        }
        String firstWord = words[0];
        if (EXCLUSIONS.contains(firstWord) || NON_SURNAMES.contains(firstWord)) {
            return false;
        }
        String lastWord = words[words.length - 1];
        if (words.length == 3) {
            return isPatronymic(lastWord) && FIRST_NAMES.contains(words[1]);
        }
        return isNameWord(lastWord);
    }

    private static boolean isPatronymic(String word) {
        return word.endsWith("ович") || word.endsWith("евна") || word.endsWith("овна")
                || word.endsWith("ич") || word.endsWith("ична");
    }
}