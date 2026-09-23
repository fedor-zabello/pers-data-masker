package ru.cs.pers_data_masker.config;

import java.util.function.Predicate;

/**
 * Реестр алгоритмов контрольных сумм для кастомных типов ПД.
 *
 * <p>Позволяет по имени алгоритма получить предикат валидации. Поддерживаются:
 * {@code luhn} (номера карт), {@code inn} (ИНН 10/12 цифр), {@code snils}
 * (СНИЛС). Перед проверкой из значения удаляются все не-цифровые символы.
 */
public final class ChecksumRegistry {

    private ChecksumRegistry() {
    }

    /**
     * Возвращает предикат валидации по имени алгоритма.
     *
     * @param name имя алгоритма (luhn, inn, snils)
     * @return предикат или {@code null}, если алгоритм неизвестен
     */
    public static Predicate<String> forName(String name) {
        if (name == null) {
            return null;
        }
        return switch (name.trim().toLowerCase()) {
            case "luhn" -> ChecksumRegistry::luhn;
            case "inn" -> ChecksumRegistry::inn;
            case "snils" -> ChecksumRegistry::snils;
            default -> null;
        };
    }

    private static String digits(String value) {
        return value.replaceAll("[^0-9]", "");
    }

    /** Алгоритм Луна (номера платёжных карт). */
    public static boolean luhn(String value) {
        String digits = digits(value);
        if (digits.length() < 2) {
            return false;
        }
        int sum = 0;
        boolean doubleDigit = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (doubleDigit) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }

    /** Контрольная сумма ИНН (10 или 12 цифр). */
    public static boolean inn(String value) {
        String digits = digits(value);
        if (digits.length() == 10) {
            return innCheck(digits, new int[]{2, 4, 10, 3, 5, 9, 4, 6, 8}, 9);
        }
        if (digits.length() == 12) {
            return innCheck(digits, new int[]{7, 2, 4, 10, 3, 5, 9, 4, 6, 8}, 10)
                    && innCheck(digits, new int[]{3, 7, 2, 4, 10, 3, 5, 9, 4, 6, 8}, 11);
        }
        return false;
    }

    private static boolean innCheck(String digits, int[] weights, int checkIndex) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += (digits.charAt(i) - '0') * weights[i];
        }
        int control = sum % 11 % 10;
        return control == (digits.charAt(checkIndex) - '0');
    }

    /** Контрольная сумма СНИЛС (11 цифр). */
    public static boolean snils(String value) {
        String digits = digits(value);
        if (digits.length() != 11) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (digits.charAt(i) - '0') * (9 - i);
        }
        int control;
        if (sum < 100) {
            control = sum;
        } else if (sum == 100 || sum == 101) {
            control = 0;
        } else {
            control = sum % 101;
            if (control == 100) {
                control = 0;
            }
        }
        int expected = Integer.parseInt(digits.substring(9));
        return control == expected;
    }
}