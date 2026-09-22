package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

import java.util.regex.Pattern;

/**
 * Детектор кода подразделения, выдавшего паспорт (формат {@code XXX-XXX}).
 */
@Component
public class PassportDepartmentCodeDetector extends AbstractRegexDetector {

    private static final String CODE_REGEX = "\\b\\d{3}-\\d{3}\\b";
    private static final Pattern CONTEXT = Pattern.compile(
            "(код\\s*подразделения|подразделение)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public PassportDepartmentCodeDetector() {
        super(CODE_REGEX, PiiType.PASSPORT_DEPARTMENT_CODE.name(), 70);
    }

    @Override
    protected boolean accept(String text, int start, int end, String original) {
        int from = Math.max(0, start - 40);
        String before = text.substring(from, start);
        return CONTEXT.matcher(before).find();
    }
}