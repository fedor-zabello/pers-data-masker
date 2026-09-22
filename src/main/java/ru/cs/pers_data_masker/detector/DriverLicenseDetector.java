package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

import java.util.regex.Pattern;

/**
 * Детектор серии и номера водительского удостоверения (4 цифры + 6 цифр)
 * с контекстной проверкой на «водительское»/«в/у».
 */
@Component
public class DriverLicenseDetector extends AbstractRegexDetector {

    private static final String LICENSE_REGEX =
            "\\b\\d{4}[\\s\\-]?\\d{6}\\b";
    private static final Pattern CONTEXT = Pattern.compile(
            "(?i)(водительск|в/у|права|удостоверение\\s*водителя)");

    public DriverLicenseDetector() {
        super(LICENSE_REGEX, PiiType.DRIVER_LICENSE.name(), 70);
    }

    @Override
    protected boolean accept(String text, int start, int end, String original) {
        int from = Math.max(0, start - 60);
        String before = text.substring(from, start);
        return CONTEXT.matcher(before).find();
    }
}