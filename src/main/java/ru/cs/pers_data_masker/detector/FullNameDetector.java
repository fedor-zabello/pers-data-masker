package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

import java.util.Set;

/**
 * Детектор ФИО / имени держателя карты (кириллица).
 *
 * <p>Ловит ФИО в разных форматах:
 * <ul>
 *   <li>полные слова: «Иванов Иван Иванович»;</li>
 *   <li>фамилия + инициалы: «Иванов И. И.», «Иванов И.И.»;</li>
 *   <li>инициалы + фамилия: «И. И. Иванов», «И.И. Иванов».</li>
 * </ul>
 *
 * <p>Словарь исключений отсекает упоминания известных лиц (поэт Пушкин и т.п.),
 * которые не являются ПД. Словарь служебных слов (роль/должность/обращение)
 * не даёт ловить «Клиент Иванов Иван» вместо «Иванов Иван Иванович».
 */
@Component
public class FullNameDetector extends AbstractRegexDetector {

    private static final String FULL_WORD = "[А-ЯЁ][а-яё]+(?:-[А-ЯЁ][а-яё]+)?";
    private static final String INITIAL = "[А-ЯЁ]\\.";

    private static final Set<String> ROLE_WORDS = Set.of(
            "Клиент", "Клиентка", "Сотрудник", "Сотрудница", "Гражданин", "Гражданка",
            "Имя", "Паспорт", "УФМС", "Отделение", "Банк", "Господин", "Госпожа",
            "Товарищ", "Доктор", "Профессор", "Директор", "Менеджер", "Специалист",
            "Инженер", "Бухгалтер", "Врач", "Учитель", "Президент", "Министр",
            "Заявитель", "Заявительница", "Пользователь", "Абонент", "Владелец",
            "Держатель", "Представитель", "Руководитель", "Начальник", "Глава"
    );

    private static final String NAME_REGEX =
            "\\b(?!(?:"
                    + String.join("|", ROLE_WORDS)
                    + ")\\b)(?:"
                    // Фамилия + (Имя Отчество | И. О. | И.О.)
                    + FULL_WORD
                    + "(?:\\s+(?:" + FULL_WORD
                    + "|" + INITIAL + "(?:\\s*" + INITIAL + ")?)){1,2}"
                    + "|"
                    // И. О. Фамилия | И.О. Фамилия
                    + INITIAL + "(?:\\s*" + INITIAL + ")?\\s+"
                    + FULL_WORD
                    + ")(?![А-ЯЁа-яё])";

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
        String[] words = original.split("\\s+");
        if (words.length == 0 || !Character.isUpperCase(words[0].charAt(0))) {
            return false;
        }
        if (ROLE_WORDS.contains(words[0])) {
            return false;
        }
        for (String w : words) {
            if (EXCLUSIONS.contains(w)) {
                return false;
            }
        }
        return true;
    }
}