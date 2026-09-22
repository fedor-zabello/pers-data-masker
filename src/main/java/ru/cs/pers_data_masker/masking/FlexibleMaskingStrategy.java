package ru.cs.pers_data_masker.masking;

import ru.cs.pers_data_masker.config.MaskingConfig;

/**
 * Общий движок маскирования для кастомных типов ПД.
 *
 * <p>Для фрагмента длиной {@code L}:
 * <ol>
 *   <li>Если {@code L <= keepStart + keepEnd} — маскируется всё.</li>
 *   <li>Иначе оставляются первые {@code keepStart} и последние {@code keepEnd}
 *       символов, середина заменяется на {@code maskChar}.</li>
 *   <li>При {@code preserveSeparators=true} не-буквенно-цифровые символы середины
 *       сохраняются, маскируются только буквы/цифры.</li>
 * </ol>
 */
public class FlexibleMaskingStrategy implements MaskingStrategy {

    private final MaskingConfig config;

    public FlexibleMaskingStrategy(MaskingConfig config) {
        this.config = config;
    }

    @Override
    public String mask(String type, String original) {
        int len = original.length();
        int keepStart = Math.min(config.keepStart(), len);
        int keepEnd = Math.min(config.keepEnd(), len - keepStart);

        if (keepStart + keepEnd >= len) {
            return maskAll(original);
        }

        String start = original.substring(0, keepStart);
        String end = original.substring(len - keepEnd);
        String middle = original.substring(keepStart, len - keepEnd);

        String maskedMiddle = config.preserveSeparators()
                ? maskPreservingSeparators(middle)
                : String.valueOf(config.maskChar()).repeat(middle.length());

        return start + maskedMiddle + end;
    }

    private String maskAll(String original) {
        return String.valueOf(config.maskChar()).repeat(original.length());
    }

    private String maskPreservingSeparators(String middle) {
        StringBuilder sb = new StringBuilder(middle.length());
        for (int i = 0; i < middle.length(); i++) {
            char c = middle.charAt(i);
            sb.append(Character.isLetterOrDigit(c) ? config.maskChar() : c);
        }
        return sb.toString();
    }
}