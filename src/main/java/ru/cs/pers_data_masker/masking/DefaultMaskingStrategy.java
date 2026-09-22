package ru.cs.pers_data_masker.masking;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Дефолтная стратегия маскирования в стиле ТЗ.
 *
 * <p>Примеры: {@code Иванов Иван Иванович} → {@code И. И. И.},
 * {@code 4509 123456} → {@code 45** ****56}, {@code ivan@mail.ru} →
 * {@code i***@mail.ru}, {@code +7 (900) 123-45-67} → {@code +7 (9**) ***-**-**},
 * {@code 1234 5678 9012 3456} → {@code 1234 **** **** 3456},
 * {@code 123456789012} → {@code 1234******12}.
 */
@Component
public class DefaultMaskingStrategy implements MaskingStrategy {

    @Override
    public String mask(String type, String original) {
        return switch (type) {
            case "FULL_NAME", "CARD_HOLDER" -> maskFullName(original);
            case "EMAIL" -> maskEmail(original);
            case "PHONE" -> maskPhone(original);
            case "CARD_NUMBER" -> maskCardNumber(original);
            case "INN" -> maskInn(original);
            case "PASSPORT", "DRIVER_LICENSE" -> maskPassport(original);
            case "CVV", "PIN" -> maskAll(original);
            case "ADDRESS" -> maskAddress(original);
            default -> maskAll(original);
        };
    }

    private static String maskFullName(String original) {
        return Arrays.stream(original.trim().split("\\s+"))
                .map(w -> w.charAt(0) + ".")
                .collect(Collectors.joining(" "));
    }

    private static String maskEmail(String original) {
        int at = original.indexOf('@');
        if (at <= 0) {
            return maskAll(original);
        }
        String local = original.substring(0, at);
        String domain = original.substring(at);
        String maskedLocal = local.charAt(0) + "*".repeat(Math.max(1, local.length() - 1));
        return maskedLocal + domain;
    }

    private static String maskPhone(String original) {
        // сохраняем "+7 (9" и маскируем остальное, сохраняя разделители
        StringBuilder sb = new StringBuilder();
        int kept = 0;
        for (int i = 0; i < original.length(); i++) {
            char c = original.charAt(i);
            if (Character.isDigit(c)) {
                if (kept < 4) {
                    sb.append(c);
                } else {
                    sb.append('*');
                }
                kept++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String maskCardNumber(String original) {
        // 1234 5678 9012 3456 -> 1234 **** **** 3456
        String[] groups = original.trim().split("\\s+");
        if (groups.length >= 4) {
            return groups[0] + " **** **** " + groups[groups.length - 1];
        }
        return maskAll(original);
    }

    private static String maskInn(String original) {
        // 123456789012 -> 1234******12
        if (original.length() >= 6) {
            return original.substring(0, 4) + "*".repeat(original.length() - 6) + original.substring(original.length() - 2);
        }
        return maskAll(original);
    }

    private static String maskPassport(String original) {
        // 4509 123456 -> 45** ****56
        String digits = original.replaceAll("[^0-9]", "");
        if (digits.length() >= 10) {
            return digits.substring(0, 2) + "** ****" + digits.substring(digits.length() - 2);
        }
        return maskAll(original);
    }

    private static String maskAddress(String original) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < original.length(); i++) {
            char c = original.charAt(i);
            sb.append(Character.isDigit(c) ? '*' : c);
        }
        return sb.toString();
    }

    private static String maskAll(String original) {
        return "*".repeat(original.length());
    }
}