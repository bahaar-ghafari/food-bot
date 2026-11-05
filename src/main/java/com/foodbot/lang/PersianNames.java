package com.foodbot.lang;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Writes Latin-script names in Persian script for Persian readers ("Bahar" -> "بهار"). Common names come
 * from a dictionary; anything else gets a best-effort phonetic transliteration. {@code @usernames},
 * digits and text that is already Persian are left alone.
 */
public final class PersianNames {
    private PersianNames() {
    }

    private static final Pattern LATIN_WORD = Pattern.compile("(?<![@\\w])[A-Za-z]+(?![\\w])");

    private static final Map<String, String> KNOWN = Map.ofEntries(
            Map.entry("bahar", "بهار"), Map.entry("behar", "بهار"), Map.entry("sara", "سارا"),
            Map.entry("sarah", "سارا"), Map.entry("ali", "علی"), Map.entry("mohammad", "محمد"),
            Map.entry("mohammed", "محمد"), Map.entry("muhammad", "محمد"), Map.entry("reza", "رضا"),
            Map.entry("hossein", "حسین"), Map.entry("hosein", "حسین"), Map.entry("hamid", "حمید"),
            Map.entry("hamed", "حامد"), Map.entry("mehdi", "مهدی"), Map.entry("mahdi", "مهدی"),
            Map.entry("amir", "امیر"), Map.entry("arash", "آرش"), Map.entry("kian", "کیان"),
            Map.entry("kaveh", "کاوه"), Map.entry("babak", "بابک"), Map.entry("behnam", "بهنام"),
            Map.entry("behzad", "بهزاد"), Map.entry("farid", "فرید"), Map.entry("farhad", "فرهاد"),
            Map.entry("payam", "پیام"), Map.entry("pouya", "پویا"), Map.entry("poya", "پویا"),
            Map.entry("saeed", "سعید"), Map.entry("said", "سعید"), Map.entry("shahab", "شهاب"),
            Map.entry("siavash", "سیاوش"), Map.entry("soheil", "سهیل"), Map.entry("sina", "سینا"),
            Map.entry("omid", "امید"), Map.entry("navid", "نوید"), Map.entry("nima", "نیما"),
            Map.entry("parsa", "پارسا"), Map.entry("peyman", "پیمان"), Map.entry("ramin", "رامین"),
            Map.entry("shervin", "شروین"), Map.entry("vahid", "وحید"), Map.entry("yasin", "یاسین"),
            Map.entry("zahra", "زهرا"), Map.entry("fatemeh", "فاطمه"), Map.entry("fatima", "فاطمه"),
            Map.entry("maryam", "مریم"), Map.entry("mina", "مینا"), Map.entry("mahsa", "مهسا"),
            Map.entry("negar", "نگار"), Map.entry("niloufar", "نیلوفر"), Map.entry("niloofar", "نیلوفر"),
            Map.entry("nasrin", "نسرین"), Map.entry("narges", "نرگس"), Map.entry("neda", "ندا"),
            Map.entry("parisa", "پریسا"), Map.entry("parinaz", "پریناز"), Map.entry("pegah", "پگاه"),
            Map.entry("roya", "رویا"), Map.entry("samira", "سمیرا"), Map.entry("shirin", "شیرین"),
            Map.entry("shiva", "شیوا"), Map.entry("somayeh", "سمیه"), Map.entry("sepideh", "سپیده"),
            Map.entry("setareh", "ستاره"), Map.entry("tina", "تینا"), Map.entry("leila", "لیلا"),
            Map.entry("laleh", "لاله"), Map.entry("elham", "الهام"), Map.entry("elnaz", "الناز"),
            Map.entry("elaheh", "الهه"), Map.entry("azadeh", "آزاده"), Map.entry("atefeh", "عاطفه"),
            Map.entry("anahita", "آناهیتا"), Map.entry("atoosa", "آتوسا"), Map.entry("donya", "دنیا"),
            Map.entry("golnaz", "گلناز"), Map.entry("hanieh", "هانیه"), Map.entry("hengameh", "هنگامه"),
            Map.entry("kimia", "کیمیا"), Map.entry("mahnaz", "مهناز"), Map.entry("marjan", "مرجان"),
            Map.entry("maral", "مارال"), Map.entry("mitra", "میترا"), Map.entry("nazanin", "نازنین"),
            Map.entry("ladan", "لادن"), Map.entry("azar", "آذر"), Map.entry("sahar", "سحر"),
            Map.entry("simin", "سیمین"), Map.entry("yalda", "یلدا"), Map.entry("zeynab", "زینب"),
            Map.entry("john", "جان"), Map.entry("david", "دیوید"), Map.entry("michael", "مایکل"),
            Map.entry("daniel", "دانیال"), Map.entry("alex", "الکس"), Map.entry("anna", "آنا"),
            Map.entry("maria", "ماریا"), Map.entry("emma", "اما"), Map.entry("lisa", "لیزا"),
            Map.entry("julia", "جولیا"), Map.entry("sofia", "سوفیا"), Map.entry("ben", "بن"));

    /** Persian-script version of the Latin words in {@code text}; everything else is kept as is. */
    public static String localize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        Matcher m = LATIN_WORD.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(out, Matcher.quoteReplacement(transliterate(m.group())));
        }
        m.appendTail(out);
        return out.toString();
    }

    /** {@link #localize} when {@code lang} is Persian, otherwise the text unchanged. */
    public static String forLang(Lang lang, String text) {
        return lang == Lang.FA ? localize(text) : text;
    }

    static String transliterate(String word) {
        String lower = word.toLowerCase();
        String known = KNOWN.get(lower);
        return known != null ? known : phonetic(lower);
    }

    private static final String VOWELS = "aeiouy";

    /** Rough letter-by-letter transliteration for names that aren't in the dictionary. */
    private static String phonetic(String w) {
        StringBuilder sb = new StringBuilder();
        int n = w.length();
        for (int i = 0; i < n; i++) {
            char c = w.charAt(i);
            String two = i + 1 < n ? w.substring(i, i + 2) : "";
            boolean first = i == 0;
            boolean last = i == n - 1;
            switch (two) {
                case "sh" -> { sb.append('ش'); i++; continue; }
                case "kh" -> { sb.append('خ'); i++; continue; }
                case "gh" -> { sb.append('ق'); i++; continue; }
                case "ch" -> { sb.append('چ'); i++; continue; }
                case "zh" -> { sb.append('ژ'); i++; continue; }
                case "ph" -> { sb.append('ف'); i++; continue; }
                case "th" -> { sb.append('ت'); i++; continue; }
                case "aa" -> { sb.append(first ? "آ" : "ا"); i++; continue; }
                case "ee", "ii" -> { sb.append(first ? "ای" : "ی"); i++; continue; }
                case "oo", "ou" -> { sb.append(first ? "او" : "و"); i++; continue; }
                default -> { }
            }
            switch (c) {
                case 'a' -> sb.append(first ? "آ" : "ا");
                case 'e' -> { if (first) sb.append('ا'); else if (last) sb.append('ه'); }
                case 'i' -> sb.append(first ? "ای" : "ی");
                case 'o' -> sb.append(first ? "ا" : "و");
                case 'u' -> sb.append(first ? "او" : "و");
                case 'y' -> sb.append(first || (i > 0 && !VOWELS.contains(w.substring(i - 1, i))) ? "ی" : "ی");
                case 'b' -> sb.append('ب');
                case 'c' -> sb.append('ک');
                case 'd' -> sb.append('د');
                case 'f' -> sb.append('ف');
                case 'g' -> sb.append('گ');
                case 'h' -> sb.append('ه');
                case 'j' -> sb.append('ج');
                case 'k' -> sb.append('ک');
                case 'l' -> sb.append('ل');
                case 'm' -> sb.append('م');
                case 'n' -> sb.append('ن');
                case 'p' -> sb.append('پ');
                case 'q' -> sb.append('ق');
                case 'r' -> sb.append('ر');
                case 's' -> sb.append('س');
                case 't' -> sb.append('ت');
                case 'v', 'w' -> sb.append('و');
                case 'x' -> sb.append("کس");
                case 'z' -> sb.append('ز');
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
