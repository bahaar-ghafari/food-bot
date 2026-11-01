package com.foodbot.food;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scales free-text ingredient amounts such as "2 cups", "1 1/2 tbsp", "½ kg" or "2-3 cloves".
 * Amounts stay free text in storage; the leading number (or range) is parsed on demand and anything
 * non-numeric ("a pinch", "to taste") is returned untouched.
 */
public final class Amount {
    private static final String NUMBER = "(?:\\d+\\s+\\d+/\\d+|\\d+/\\d+|\\d+(?:\\.\\d+)?\\s*[½¼¾⅓⅔]|\\d+(?:\\.\\d+)?|[½¼¾⅓⅔])";
    private static final Pattern LEADING = Pattern.compile(
            "^(" + NUMBER + ")(?:(\\s*[-–]\\s*)(" + NUMBER + "))?(?![\\d/.])(.*)$", Pattern.DOTALL);

    private Amount() {
    }

    /** Returns {@code amount} multiplied by {@code factor}, or unchanged when it has no leading number. */
    public static String scale(String amount, double factor) {
        if (amount == null || factor == 1.0) {
            return amount;
        }
        boolean persianDigits = amount.chars().anyMatch(c -> c >= '۰' && c <= '۹');
        Matcher m = LEADING.matcher(asciiDigits(amount).trim());
        if (!m.matches()) {
            return amount;
        }
        String result = format(parse(m.group(1)) * factor);
        if (m.group(3) != null) {
            result += m.group(2).trim() + format(parse(m.group(3)) * factor);
        }
        result += m.group(4);
        return persianDigits ? persianDigits(result) : result;
    }

    static double parse(String number) {
        String s = number.trim();
        double total = 0;
        if (!s.isEmpty() && "½¼¾⅓⅔".indexOf(s.charAt(s.length() - 1)) >= 0) {
            total += switch (s.charAt(s.length() - 1)) {
                case '½' -> 0.5;
                case '¼' -> 0.25;
                case '¾' -> 0.75;
                case '⅓' -> 1.0 / 3;
                default -> 2.0 / 3;
            };
            s = s.substring(0, s.length() - 1).trim();
            if (s.isEmpty()) {
                return total;
            }
        }
        String[] parts = s.split("\\s+");
        for (String part : parts) {
            int slash = part.indexOf('/');
            if (slash > 0) {
                total += Double.parseDouble(part.substring(0, slash)) / Double.parseDouble(part.substring(slash + 1));
            } else {
                total += Double.parseDouble(part);
            }
        }
        return total;
    }

    public static String format(double value) {
        BigDecimal rounded = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        return rounded.toPlainString();
    }

    private static String asciiDigits(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            b.append(c >= '۰' && c <= '۹' ? (char) ('0' + (c - '۰')) : c);
        }
        return b.toString();
    }

    private static String persianDigits(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            b.append(c >= '0' && c <= '9' ? (char) ('۰' + (c - '0')) : c);
        }
        return b.toString();
    }
}
