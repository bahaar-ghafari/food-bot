package com.foodbot.lang;

public final class TimeFormat {

    public static String format(int minutes, Lang lang) {
        if (minutes < 60) {
            return minutes + " " + Messages.get(lang, "min_unit");
        }

        int days = minutes / 1440;
        int remainderAfterDays = minutes % 1440;
        int hours = remainderAfterDays / 60;
        int mins = remainderAfterDays % 60;

        StringBuilder result = new StringBuilder();
        if (days > 0) {
            result.append(days).append(" ").append(Messages.get(lang, days == 1 ? "time.day" : "time.days"));
        }
        if (hours > 0) {
            appendSeparator(result, lang);
            result.append(hours).append(" ").append(Messages.get(lang, hours == 1 ? "time.hour" : "time.hours"));
        }
        if (mins > 0) {
            appendSeparator(result, lang);
            result.append(mins).append(" ").append(Messages.get(lang, "min_unit"));
        }
        return result.toString();
    }

    private static void appendSeparator(StringBuilder builder, Lang lang) {
        if (builder.length() == 0) {
            return;
        }
        builder.append(lang == Lang.FA ? " و " : " ");
    }

    private TimeFormat() {
    }
}
