package com.foodbot.importer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rule-based recipe reader for pasted text and OCR output, in English or Persian. It looks for the usual
 * section headings ("Ingredients", "Instructions", "مواد لازم", "طرز تهیه" ...), falls back to the shape of
 * the lines when there are none, and splits each ingredient line into a name and an amount. It's a best
 * effort: the user always checks and fixes the result on the review screen before anything is saved.
 */
public final class RecipeTextParser {
    private enum Section { NONE, INGREDIENTS, STEPS, SKIP }

    /** {@code title}: a dish name inside the heading itself ("مواد لازم برای تهیه کیک اسفنجی"), or null. */
    private record Heading(Section section, String rest, String title) {
        Heading(Section section, String rest) {
            this(section, rest, null);
        }
    }

    // Headings that may be followed by more words ("Ingredients for 4 people").
    private static final List<String> INGREDIENT_HEADINGS = List.of("ingredients", "ingredient", "you will need",
            "youll need", "what you need", "shopping list", "مواد لازم", "مواد اولیه", "مواد مورد نیاز", "مواد لازم برای");
    private static final List<String> STEP_HEADINGS_PREFIX = List.of("instructions", "instruction", "directions",
            "direction", "method", "preparation");
    // Headings that must be the whole line: many dish titles start with them ("طرز تهیه کیک شکلاتی").
    private static final List<String> EXACT_INGREDIENT_HEADINGS = List.of("مواد", "لوازم", "مواد مورد نیاز");
    private static final List<String> EXACT_STEP_HEADINGS = List.of("steps", "how to make", "how to make it",
            "how to cook", "how to cook it", "procedure", "طرز تهیه", "دستور پخت", "روش تهیه", "روش پخت",
            "طرز پخت", "مراحل", "مراحل تهیه", "مراحل پخت", "دستور تهیه", "روش کار", "طرز درست کردن", "دستور");
    private static final List<String> SKIP_HEADINGS = List.of("notes", "note", "tips", "tip", "nutrition",
            "nutrition facts", "nutrition information", "equipment", "storage", "serving suggestions",
            "نکات", "نکته", "نکته ها", "ارزش غذایی", "نکات مهم");

    private static final String AFTER = "(?=\\s|$|[.,،:;)])";
    private static final String D = "[0-9\\u06F0-\\u06F9\\u0660-\\u0669]";
    private static final String FRACTION = "[½¼¾⅓⅔⅛⅜⅝⅞]";
    private static final String NUMBER = "(?:" + D + "+\\s+" + D + "+/" + D + "+|" + D + "+(?:[.,/٫]" + D + "+)?\\s*"
            + FRACTION + "?|" + FRACTION + ")";
    private static final String WORD_NUMBER = "(?:a few|a couple of|an|a|one|two|three|four|five|six|seven|eight|nine"
            + "|ten|twelve|half|یک|دو|سه|چهار|پنج|شش|هفت|هشت|نه|ده|نیم|نصف|چند)";
    private static final String QTY = "(?:" + NUMBER + "(?:\\s*(?:-|–|to|تا)\\s*" + NUMBER + ")?|" + WORD_NUMBER + AFTER + ")";
    private static final String UNIT = "(?:tablespoons?|tbsps?|tbs|tbl|teaspoons?|tsps?|cups?|grams?|gr|g|kilograms?"
            + "|kilos?|kg|milliliters?|millilitres?|ml|liters?|litres?|l|ounces?|oz|pounds?|lbs?|pinch(?:es)?|dash(?:es)?"
            + "|cloves?|cans?|tins?|slices?|pieces?|pcs|handfuls?|bunch(?:es)?|sticks?|packages?|packets?|pkgs?|sprigs?"
            + "|stalks?|heads?|drops?|jars?|bottles?|sheets?|leaves|cubes?|fillets?|knobs?"
            + "|قاشق غذاخوری|قاشق غذا خوری|قاشق چای\\u200cخوری|قاشق چایخوری|قاشق چای خوری|قاشق مرباخوری|قاشق"
            + "|لیوان|پیمانه|عدد|گرم|کیلوگرم|کیلو|میلی\\u200cلیتر|میلی لیتر|میلیلیتر|لیتر|سی سی|حبه|بسته|قوطی|برش"
            + "|ورق|کف دست|مشت|دسته|پیاله|استکان|فنجان|تکه|بوته)";
    private static final String FREE_AMOUNT = "(?:to taste|as needed|as required|for serving|for garnish|optional"
            + "|به مقدار لازم|به مقدار کافی|به میزان لازم|به میزان کافی|به اندازه لازم|به اندازه کافی|به دلخواه|کمی"
            + "|مقداری|مقدار لازم|مقدار کافی|برای تزیین)";

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS;
    private static final Pattern LEADING_AMOUNT = Pattern.compile(
            "^(" + QTY + "(?:\\s*" + UNIT + "\\.?" + AFTER + ")?(?:\\s*" + UNIT + "\\.?" + AFTER + ")?)\\s*(?:of\\s+)?(.+)$",
            FLAGS);
    private static final Pattern LEADING_UNIT_ONLY = Pattern.compile(
            "^(" + UNIT + ")\\s+(?:of\\s+)(.+)$", FLAGS);
    private static final Pattern TRAILING_AMOUNT = Pattern.compile(
            "^(.+?)(?:\\s*[:\\-–,،]\\s*|\\s+)(" + QTY + "\\s*(?:" + UNIT + "\\.?)?(?:\\s*" + UNIT + ")?|" + FREE_AMOUNT + ")"
                    + "\\s*[.]?$", FLAGS);
    private static final Pattern FREE_AMOUNT_ANYWHERE = Pattern.compile("\\s*[,،\\-–]?\\s*" + FREE_AMOUNT + "\\s*", FLAGS);
    private static final Pattern STARTS_WITH_QTY = Pattern.compile("^(?:" + NUMBER + ")" + AFTER + "|^" + D + "+"
            + UNIT + AFTER, FLAGS);
    private static final Pattern STEP_NUMBER = Pattern.compile(
            "^(?:(?:step|مرحله|گام)\\s*)?" + D + "{1,2}\\s*[.):\\-–]\\s*|^(?:step|مرحله|گام)\\s*" + D + "{1,2}\\s*",
            FLAGS);
    private static final Pattern BULLET = Pattern.compile("^[\\-–—•·*▢☐□◦○●▪✓✔➤>]+\\s*");
    private static final Pattern LEADING_SYMBOLS = Pattern.compile("^[^\\p{L}\\p{N}(]+", FLAGS);
    private static final Pattern PARENTHESES = Pattern.compile("\\s*[(\\[][^)\\]]*[)\\]]");
    private static final Pattern HOURS = Pattern.compile(
            "(" + D + "+(?:[.,]" + D + "+)?\\s*" + FRACTION + "?|" + FRACTION + "|an|one|a|half|نیم|یک|یه)\\s*"
                    + "(?:hours?|hrs?|h|ساعت)" + AFTER + "(\\s*و\\s*نیم)?", FLAGS);
    private static final Pattern MINUTES = Pattern.compile(
            "(" + D + "+)(?:\\s*[-–]\\s*(" + D + "+))?\\s*(?:minutes?|mins?|m|دقیقه|دقيقه)" + AFTER, FLAGS);
    private static final Pattern SERVINGS = Pattern.compile(
            "^(?:serves|servings?|yield|yields|makes|portions?|calories|kcal|difficulty|برای|تعداد|کالری|سختی)\\b"
                    + "|" + D + "+\\s*نفر|^(?:prep|cook|total|active|ready in|زمان)\\b", FLAGS);
    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?؟])\\s+");

    private static final List<String> EN_LEADING_DESCRIPTORS = List.of("fresh", "large", "small", "medium", "big",
            "chopped", "diced", "minced", "sliced", "grated", "finely", "roughly", "peeled", "crushed", "melted",
            "softened", "ripe", "about", "approx", "approximately", "heaping", "heaped", "level", "packed");
    private static final List<String> FA_TRAILING_DESCRIPTORS = List.of("رنده شده", "خرد شده", "خلال شده", "ریز شده",
            "پوست کنده", "حلقه شده", "نگینی", "تازه", "متوسط", "بزرگ", "کوچک", "درشت", "ریز", "دلخواه", "آب شده",
            "ذوب شده", "پخته شده", "پخته", "له شده");

    private RecipeTextParser() {
    }

    /**
     * @param caption the user's caption on a screenshot, preferred as the dish name; may be null
     * @return empty when nothing in the text looks like a recipe
     */
    public static Optional<ImportedRecipe> parse(String text, String caption, List<String> knownIngredients) {
        List<String> preamble = new ArrayList<>();
        List<String> ingredientLines = new ArrayList<>();
        List<String> stepLines = new ArrayList<>();
        boolean sawHeading = false;
        String headingTitle = null;
        Section section = Section.NONE;
        for (String raw : clean(text).split("\\R")) {
            String line = raw.strip();
            if (line.isEmpty()) {
                continue;
            }
            Heading heading = heading(line);
            if (heading != null) {
                sawHeading = sawHeading || heading.section() != Section.SKIP;
                if (headingTitle == null) {
                    headingTitle = heading.title();
                }
                section = heading.section();
                if (!heading.rest().isEmpty()) {
                    if (section == Section.INGREDIENTS) {
                        // "Ingredients: 3 eggs, butter, salt" - one comma-separated line.
                        for (String part : heading.rest().split("[,،]")) {
                            if (!part.isBlank()) {
                                ingredientLines.add(part.strip());
                            }
                        }
                    } else if (section == Section.STEPS) {
                        stepLines.add(heading.rest());
                    }
                }
                continue;
            }
            switch (section) {
                case NONE -> preamble.add(line);
                case INGREDIENTS -> ingredientLines.add(line);
                case STEPS -> stepLines.add(line);
                case SKIP -> {
                }
            }
        }

        // The title line is never an ingredient, even when the caption supplies the name.
        int titleIndex = titleLineIndex(preamble);
        List<String> body = new ArrayList<>(preamble);
        if (titleIndex >= 0) {
            body.remove(titleIndex);
        }
        String name = captionName(caption);
        if (name == null && titleIndex >= 0) {
            name = cleanName(preamble.get(titleIndex));
        }
        if (name == null) {
            name = headingTitle;
        }
        boolean fallback = false;
        if (ingredientLines.isEmpty()) {
            // No ingredient heading: take the list-like lines before the method starts.
            fallback = !sawHeading;
            List<String> rest = new ArrayList<>();
            boolean inMethod = false;
            for (String line : body) {
                if (isMetaLine(line)) {
                    continue;
                }
                if (!inMethod && looksLikeIngredientLine(line)) {
                    ingredientLines.add(line);
                } else {
                    inMethod = true;
                    rest.add(line);
                }
            }
            if (stepLines.isEmpty()) {
                stepLines.addAll(rest);
            }
        } else if (stepLines.isEmpty()) {
            // An ingredient heading but no method heading: the method starts at the first sentence-like line.
            for (int i = 0; i < ingredientLines.size(); i++) {
                if (looksLikeSentence(ingredientLines.get(i))) {
                    stepLines.addAll(ingredientLines.subList(i, ingredientLines.size()));
                    ingredientLines = new ArrayList<>(ingredientLines.subList(0, i));
                    break;
                }
            }
        }

        List<ImportedRecipe.Ingredient> ingredients = new ArrayList<>();
        for (String line : ingredientLines) {
            ImportedRecipe.Ingredient ingredient = parseIngredient(line, knownIngredients);
            if (ingredient != null) {
                ingredients.add(ingredient);
            }
        }
        if (ingredients.isEmpty()) {
            return Optional.empty();
        }
        // Copied recipes often start straight at "Ingredients": import anyway and let the user name it.
        if (name == null) {
            name = "";
        }
        // Without any headings, only trust it if it really reads like an ingredient list.
        if (fallback && (ingredients.size() < 2 || ingredients.stream().allMatch(i -> i.amount().isEmpty()))) {
            return Optional.empty();
        }
        List<String> timeLines = preamble.stream().filter(RecipeTextParser::isMetaLine).toList();
        return Optional.of(new ImportedRecipe(name, prepTime(timeLines), CategoryGuesser.guess(name, text),
                ingredients, parseSteps(stepLines)));
    }

    /** Drops invisible direction marks and the like that OCR and copy-paste leave behind (keeps the ZWNJ). */
    static String clean(String text) {
        // Many Persian sites type the Arabic forms of ی and ک, which would otherwise miss every Persian keyword.
        return text.replace('ي', 'ی').replace('ى', 'ی').replace('ك', 'ک')
                .replaceAll("[\\u200B\\u200E\\u200F\\u202A-\\u202E\\u2066-\\u2069\\uFEFF]", "")
                .replace(' ', ' ').replace('\t', ' ');
    }

    static Heading heading(String line) {
        String beforeColon = line;
        String rest = "";
        int colon = indexOfColon(line);
        if (colon >= 0) {
            beforeColon = line.substring(0, colon);
            rest = line.substring(colon + 1).strip();
        }
        String key = headingKey(beforeColon);
        if (key.isEmpty() || key.length() > 60) {
            return null;
        }
        if (EXACT_INGREDIENT_HEADINGS.contains(key) || startsWithAny(key, INGREDIENT_HEADINGS)) {
            return new Heading(Section.INGREDIENTS, rest, titleInHeading(key));
        }
        if (key.length() > 40) {
            return null;
        }
        if (EXACT_STEP_HEADINGS.contains(key) || startsWithAny(key, STEP_HEADINGS_PREFIX)) {
            return new Heading(Section.STEPS, rest);
        }
        if (SKIP_HEADINGS.contains(key) || (colon >= 0 && startsWithAny(key, SKIP_HEADINGS))) {
            return new Heading(Section.SKIP, rest);
        }
        return null;
    }

    private static final Pattern HEADING_TITLE = Pattern.compile(
            "^(?:ingredients?|مواد لازم|مواد اولیه|مواد مورد نیاز)\\s+(?:for\\s+(?:the\\s+|a\\s+)?|برای\\s+(?:تهیه\\s+|درست کردن\\s+)?)(.+)$");
    private static final Pattern SERVING_WORDS = Pattern.compile(
            "(?:^|\\s)(?:people|persons?|servings?|serves|portions?|نفر|پرس)(?:\\s|$)");

    /** "Ingredients for chocolate cake" / "مواد لازم برای تهیه کیک" -> the dish; not "for 4 people". */
    static String titleInHeading(String key) {
        Matcher m = HEADING_TITLE.matcher(key);
        if (!m.matches()) {
            return null;
        }
        String title = m.group(1).strip();
        return title.length() >= 3 && !SERVING_WORDS.matcher(title).find() ? title : null;
    }

    private static int indexOfColon(String line) {
        int ascii = line.indexOf(':');
        int arabic = line.indexOf('：');
        if (ascii < 0) {
            return arabic;
        }
        return arabic < 0 ? ascii : Math.min(ascii, arabic);
    }

    private static String headingKey(String text) {
        String key = text.toLowerCase(Locale.ROOT).replace("‌", " ").replace("'", "").replace("’", "");
        key = key.replaceAll("[^\\p{L}\\s]", " ").replaceAll("\\s+", " ").strip();
        return key;
    }

    private static boolean startsWithAny(String key, List<String> phrases) {
        for (String phrase : phrases) {
            if (key.equals(phrase) || key.startsWith(phrase + " ")) {
                return true;
            }
        }
        return false;
    }

    private static String captionName(String caption) {
        if (caption == null) {
            return null;
        }
        String c = cleanName(caption.strip());
        return !c.isEmpty() && !c.contains("\n") && c.length() <= ImportedRecipe.MAX_NAME_LENGTH ? c : null;
    }

    /** The first line that reads like a title, skipping OCR noise such as a phone's status bar or a @handle. */
    private static int titleLineIndex(List<String> preamble) {
        for (int i = 0; i < preamble.size(); i++) {
            String line = preamble.get(i);
            if (isMetaLine(line) || line.length() > ImportedRecipe.MAX_NAME_LENGTH
                    || STARTS_WITH_QTY.matcher(stripListMarker(line)).find()) {
                continue;
            }
            String raw = line.strip();
            if (raw.startsWith("@") || raw.startsWith("#") || raw.contains("http") || raw.contains("www.")) {
                continue;
            }
            String name = cleanName(raw);
            long letters = name.codePoints().filter(Character::isLetter).count();
            if (letters >= 2 && letters * 2 >= name.length()) {
                return i;
            }
        }
        return -1;
    }

    static String cleanName(String line) {
        String name = LEADING_SYMBOLS.matcher(line).replaceFirst("");
        name = name.replaceAll("[\\p{So}\\p{Cn}\\x{1F000}-\\x{1FFFF}\\uFE0F]", "").strip();
        name = name.replaceAll("(?i)^(?:recipe\\s*[:\\-–]\\s*|how to make\\s+|easy\\s+|the best\\s+|best\\s+)", "");
        name = name.replaceAll("(?i)\\s+recipe$", "");
        name = name.replaceAll("^(?:طرز تهیه|دستور پخت|روش تهیه|طرز پخت|دستور تهیه|طرز درست کردن)\\s+", "");
        name = name.replaceAll("[\\s.:!،,\\-–]+$", "");
        return name.strip();
    }

    static boolean isMetaLine(String line) {
        String l = line.strip();
        if (SERVINGS.matcher(l).find()) {
            return true;
        }
        return l.length() < 50 && (HOURS.matcher(l).find() || MINUTES.matcher(l).find())
                && !LEADING_AMOUNT.matcher(stripListMarker(l)).matches();
    }

    private static boolean looksLikeIngredientLine(String line) {
        String l = stripListMarker(line);
        if (STARTS_WITH_QTY.matcher(l).find()) {
            return l.length() <= 80;
        }
        return l.length() <= 30 && l.split("\\s+").length <= 4 && !l.endsWith(".") && !l.endsWith(":");
    }

    private static boolean looksLikeSentence(String line) {
        String l = stripListMarker(line);
        return l.length() > 70 || (l.length() > 35 && (l.endsWith(".") || l.contains(". ")));
    }

    private static String stripListMarker(String line) {
        String l = BULLET.matcher(line.strip()).replaceFirst("");
        // "1. 2 eggs" / "1) flour": list numbering, not an amount, when another number or word follows.
        l = l.replaceFirst("^" + D + "{1,2}[.)]\\s+(?=\\S)", "");
        return l.strip();
    }

    static ImportedRecipe.Ingredient parseIngredient(String line, List<String> knownIngredients) {
        String s = stripListMarker(line);
        s = LEADING_SYMBOLS.matcher(s).replaceFirst("").strip();
        if (s.isEmpty() || s.endsWith(":") || isMetaLine(s)) {
            return null;
        }
        String amount = "";
        String name = s;
        Matcher leading = LEADING_AMOUNT.matcher(s);
        Matcher unitOnly = LEADING_UNIT_ONLY.matcher(s);
        if (leading.matches() && !leading.group(2).isBlank()) {
            amount = leading.group(1).strip();
            name = leading.group(2);
        } else if (unitOnly.matches()) {
            amount = unitOnly.group(1).strip();
            name = unitOnly.group(2);
        } else {
            Matcher trailing = TRAILING_AMOUNT.matcher(s);
            if (trailing.matches() && !trailing.group(1).isBlank()) {
                name = trailing.group(1);
                amount = trailing.group(2).strip();
            }
        }
        // "Salt and pepper, to taste" / "1 pinch salt (to taste)"
        Matcher free = FREE_AMOUNT_ANYWHERE.matcher(name);
        if (free.find()) {
            String found = free.group().replaceAll("^[\\s,،\\-–]+", "").strip();
            amount = amount.isEmpty() ? found : amount;
            name = free.replaceAll(" ");
        }
        if (amount.equalsIgnoreCase("a") || amount.equalsIgnoreCase("an")) {
            amount = "1";
        }
        name = cleanIngredientName(name);
        if (name.isEmpty() || name.length() > 40 || name.codePoints().noneMatch(Character::isLetter)) {
            return null;
        }
        return new ImportedRecipe.Ingredient(canonical(name, knownIngredients), amount);
    }

    private static String cleanIngredientName(String raw) {
        String name = PARENTHESES.matcher(raw).replaceAll("");
        int comma = name.indexOf(',');
        if (comma > 0) {
            name = name.substring(0, comma); // "onions, finely chopped"
        }
        int persianComma = name.indexOf('،');
        if (persianComma > 0) {
            name = name.substring(0, persianComma);
        }
        name = name.replaceAll("(?i)^(?:of|the)\\s+", "").replaceAll("[\\s.:;\\-–*]+$", "").strip();
        if (isLatin(name)) {
            name = name.toLowerCase(Locale.ROOT);
            boolean changed = true;
            while (changed) {
                changed = false;
                for (String descriptor : EN_LEADING_DESCRIPTORS) {
                    if (name.startsWith(descriptor + " ")) {
                        name = name.substring(descriptor.length() + 1).strip();
                        changed = true;
                    }
                }
            }
        } else {
            boolean changed = true;
            while (changed) {
                changed = false;
                for (String descriptor : FA_TRAILING_DESCRIPTORS) {
                    if (name.endsWith(" " + descriptor)) {
                        name = name.substring(0, name.length() - descriptor.length() - 1).strip();
                        changed = true;
                    }
                }
            }
        }
        return name.replaceAll("\\s+", " ").strip();
    }

    private static boolean isLatin(String s) {
        return s.chars().anyMatch(c -> Character.isLetter(c) && c < 128);
    }

    /** Reuses the list's spelling ("Eggs" -> "egg") so the same ingredient isn't stored twice. */
    static String canonical(String name, List<String> knownIngredients) {
        for (String candidate : List.of(name, singular(name))) {
            for (String known : knownIngredients) {
                if (known.equalsIgnoreCase(candidate)) {
                    return known;
                }
            }
        }
        return isLatin(name) ? singular(name) : name;
    }

    static String singular(String name) {
        int space = name.lastIndexOf(' ');
        String head = space < 0 ? "" : name.substring(0, space + 1);
        String word = name.substring(space + 1);
        String lower = word.toLowerCase(Locale.ROOT);
        if (!isLatin(word) || lower.length() <= 3 || lower.endsWith("ss") || lower.endsWith("sses") || lower.endsWith("us")
                || lower.endsWith("is")) {
            return name;
        }
        if (lower.endsWith("ies")) {
            return head + word.substring(0, word.length() - 3) + "y";
        }
        if (lower.endsWith("oes") || lower.endsWith("ches") || lower.endsWith("shes") || lower.endsWith("xes")) {
            return head + word.substring(0, word.length() - 2);
        }
        if (lower.endsWith("ves") && !lower.equals("olives") && !lower.equals("chives") && !lower.equals("cloves")) {
            return head + word.substring(0, word.length() - 3) + "f"; // halves -> half, loaves -> loaf
        }
        if (lower.endsWith("s")) {
            return head + word.substring(0, word.length() - 1);
        }
        return name;
    }

    static List<String> parseSteps(List<String> lines) {
        List<String> steps = new ArrayList<>();
        long numbered = lines.stream().filter(l -> STEP_NUMBER.matcher(l.strip()).find()).count();
        long bulleted = lines.stream().filter(l -> BULLET.matcher(l.strip()).find()).count();
        if (numbered >= 2 || bulleted >= 2) {
            // Each numbered/bulleted line starts a step; the lines between are its wrapped continuation.
            Pattern marker = numbered >= 2 ? STEP_NUMBER : BULLET;
            StringBuilder current = null;
            for (String line : lines) {
                String l = line.strip();
                Matcher m = marker.matcher(l);
                if (m.find()) {
                    if (current != null) {
                        steps.add(current.toString().strip());
                    }
                    current = new StringBuilder(l.substring(m.end()));
                } else if (current == null) {
                    current = new StringBuilder(l);
                } else {
                    current.append(' ').append(l);
                }
            }
            if (current != null) {
                steps.add(current.toString().strip());
            }
        } else {
            // A paragraph (or OCR's wrapped lines): one step per sentence.
            String joined = String.join(" ", lines.stream().map(String::strip).toList());
            for (String sentence : SENTENCE_END.split(joined)) {
                steps.add(sentence);
            }
        }
        List<String> result = new ArrayList<>();
        for (String step : steps) {
            String s = LEADING_SYMBOLS.matcher(step).replaceFirst("").replaceAll("\\s+", " ").strip();
            if (s.codePoints().filter(Character::isLetter).count() >= 2) {
                result.add(s);
            }
        }
        return result;
    }

    /**
     * Total time in minutes from the lines before the ingredients: a "total" line wins, otherwise prep + cook,
     * otherwise the first time mentioned. 0 when there's none (the draft then gets a default).
     */
    static int prepTime(List<String> lines) {
        int prepAndCook = 0;
        int first = 0;
        for (String line : lines) {
            int minutes = minutesIn(line);
            if (minutes <= 0) {
                continue;
            }
            String l = line.toLowerCase(Locale.ROOT);
            if (l.contains("total") || l.contains("ready in") || l.contains("کل") || l.contains("مجموع")) {
                return minutes;
            }
            if (l.contains("prep") || l.contains("cook") || l.contains("آماده") || l.contains("پخت")) {
                prepAndCook += minutes;
            }
            if (first == 0) {
                first = minutes;
            }
        }
        return prepAndCook > 0 ? prepAndCook : first;
    }

    static int minutesIn(String line) {
        String l = asciiDigits(line);
        double total = 0;
        Matcher h = HOURS.matcher(l);
        while (h.find()) {
            total += 60 * hoursValue(h.group(1));
            if (h.group(2) != null) {
                total += 30;
            }
        }
        Matcher m = MINUTES.matcher(l);
        while (m.find()) {
            total += Integer.parseInt(m.group(2) != null ? m.group(2) : m.group(1));
        }
        return (int) Math.round(total);
    }

    private static double hoursValue(String text) {
        String t = text.strip().toLowerCase(Locale.ROOT);
        switch (t) {
            case "an", "a", "one", "یک", "یه":
                return 1;
            case "half", "نیم", "½":
                return 0.5;
            default:
                break;
        }
        double value = 0;
        char last = t.charAt(t.length() - 1);
        if ("½¼¾".indexOf(last) >= 0) {
            value += last == '½' ? 0.5 : last == '¼' ? 0.25 : 0.75;
            t = t.substring(0, t.length() - 1).strip();
        }
        if (!t.isEmpty()) {
            value += Double.parseDouble(t.replace(',', '.'));
        }
        return value;
    }

    static String asciiDigits(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c >= '۰' && c <= '۹') {
                b.append((char) ('0' + (c - '۰')));
            } else if (c >= '٠' && c <= '٩') {
                b.append((char) ('0' + (c - '٠')));
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }
}
