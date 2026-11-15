package com.foodbot.importer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeTextParserTest {
    private static final List<String> KNOWN = List.of("egg", "onion", "flour", "milk", "پیاز", "برنج");

    private static ImportedRecipe parse(String text) {
        return RecipeTextParser.parse(text, null, KNOWN).orElseThrow();
    }

    private static ImportedRecipe.Ingredient ing(String name, String amount) {
        return new ImportedRecipe.Ingredient(name, amount);
    }

    @Test
    void readsATypicalWebRecipe() {
        ImportedRecipe r = parse("""
                Classic Pancakes
                Prep time: 10 minutes
                Cook time: 15 mins
                Serves 4

                Ingredients
                - 1 1/2 cups flour
                - 2 large eggs
                - 1 ¼ cups milk
                - 3 tbsp butter, melted
                - 1 pinch of salt
                - Maple syrup, to taste

                Instructions
                1. Whisk the flour and salt in a bowl.
                2. Beat in the eggs and milk until smooth. Rest the batter for
                5 minutes.
                3. Cook in a hot buttered pan until golden.

                Notes
                Keeps 2 days.
                """);

        assertEquals("Classic Pancakes", r.name());
        assertEquals(25, r.prepTimeMinutes(), "prep + cook");
        assertEquals("Breakfast", r.category());
        assertEquals(List.of(ing("flour", "1 1/2 cups"), ing("egg", "2"), ing("milk", "1 ¼ cups"),
                ing("butter", "3 tbsp"), ing("salt", "1 pinch"), ing("maple syrup", "to taste")), r.ingredients());
        assertEquals(List.of("Whisk the flour and salt in a bowl.",
                "Beat in the eggs and milk until smooth. Rest the batter for 5 minutes.",
                "Cook in a hot buttered pan until golden."), r.steps(), "wrapped line joins its step; notes skipped");
    }

    @Test
    void readsAPersianRecipeWithAmountsAfterTheName() {
        ImportedRecipe r = parse("""
                طرز تهیه قورمه سبزی
                زمان پخت: ۳ ساعت
                مواد لازم:
                گوشت گوسفندی ۵۰۰ گرم
                ۲ عدد پیاز متوسط
                لیمو عمانی ۴ عدد
                نمک و فلفل به مقدار لازم
                طرز تهیه:
                ۱- پیاز را خرد کرده و تفت دهید.
                ۲- گوشت را اضافه کنید و سرخ کنید.
                """);

        assertEquals("قورمه سبزی", r.name(), "the 'طرز تهیه' title prefix is dropped, not taken as a heading");
        assertEquals(180, r.prepTimeMinutes());
        assertEquals("MainCourse", r.category());
        assertEquals(List.of(ing("گوشت گوسفندی", "۵۰۰ گرم"), ing("پیاز", "۲ عدد"), ing("لیمو عمانی", "۴ عدد"),
                ing("نمک و فلفل", "به مقدار لازم")), r.ingredients());
        assertEquals(List.of("پیاز را خرد کرده و تفت دهید.", "گوشت را اضافه کنید و سرخ کنید."), r.steps());
    }

    @Test
    void readsAChatMessageWithoutHeadings() {
        ImportedRecipe r = parse("""
                Tomato pasta
                200g spaghetti
                2 tomatoes
                1 onion
                salt
                Boil the pasta. Fry the onion, add the tomatoes and simmer 10 minutes. Mix with the pasta.
                """);

        assertEquals("Tomato pasta", r.name());
        assertEquals(0, r.prepTimeMinutes(), "a time inside the method isn't the total");
        assertEquals(List.of(ing("spaghetti", "200g"), ing("tomato", "2"), ing("onion", "1"), ing("salt", "")),
                r.ingredients());
        assertEquals(3, r.steps().size());
    }

    @Test
    void readsInlineSections() {
        ImportedRecipe r = parse("Omelette\nIngredients: 3 eggs, butter, chives\nMethod: Beat the eggs. Cook in butter.");

        assertEquals(List.of(ing("egg", "3"), ing("butter", ""), ing("chive", "")), r.ingredients());
        assertEquals(List.of("Beat the eggs.", "Cook in butter."), r.steps());
    }

    @Test
    void skipsScreenshotNoiseBeforeTheTitleAndPrefersTheCaption() {
        String ocr = "9:41\n@chef.sara\nBanana Bread\nIngredients\n3 bananas\n2 cups flour\nMethod\nMash and bake.";
        assertEquals("Banana Bread", parse(ocr).name());

        ImportedRecipe captioned = RecipeTextParser.parse(ocr, "Mum's banana bread", KNOWN).orElseThrow();
        assertEquals("Mum's banana bread", captioned.name());
        assertEquals(List.of(ing("banana", "3"), ing("flour", "2 cups")), captioned.ingredients());
    }

    @Test
    void recipeWithoutATitleIsStillImportedWithAnEmptyName() {
        ImportedRecipe fa = parse("مواد لازم:\nآرد ۲ پیمانه\nشکر ۱ پیمانه\nطرز تهیه:\nمخلوط کنید.");
        assertEquals("", fa.name());
        assertEquals(List.of(ing("آرد", "۲ پیمانه"), ing("شکر", "۱ پیمانه")), fa.ingredients());

        ImportedRecipe servings = parse("مواد لازم برای ۴ نفر:\nبرنج ۳ پیمانه\nطرز تهیه:\nبپزید.");
        assertEquals("", servings.name(), "'for 4 people' isn't a dish name");
        assertEquals("", parse("Ingredients:\n2 cups flour\n3 eggs\nInstructions:\nBake.").name());
    }

    @Test
    void dishNameIsTakenFromTheIngredientsHeading() {
        assertEquals("کیک اسفنجی", parse("مواد لازم برای تهیه کیک اسفنجی:\nآرد ۲ پیمانه\nطرز تهیه:\nبپزید.").name());
        assertEquals("chocolate cake", parse("Ingredients for the chocolate cake\n2 cups flour\nMethod\nBake.").name());
    }

    @Test
    void arabicFormsOfPersianLettersAreUnderstood() {
        ImportedRecipe r = parse("کيک اسفنجي\nمواد اوليه:\nآرد ۲ پيمانه\nطرز تهيه:\nمخلوط کنيد.");

        assertEquals("کیک اسفنجی", r.name());
        assertEquals(List.of(ing("آرد", "۲ پیمانه")), r.ingredients());
        assertEquals(List.of("مخلوط کنید."), r.steps());
    }

    @Test
    void shortPasteWithACommaSeparatedIngredientLine() {
        ImportedRecipe withAmounts = parse("قورمه سبزی\nسبزی قورمه ۵۰۰ گرم، گوشت ۳۰۰ گرم، لوبیا قرمز ۱ پیمانه، پیاز ۱ عدد");
        assertEquals("قورمه سبزی", withAmounts.name());
        assertEquals(List.of(ing("سبزی قورمه", "۵۰۰ گرم"), ing("گوشت", "۳۰۰ گرم"), ing("لوبیا قرمز", "۱ پیمانه"),
                ing("پیاز", "۱ عدد")), withAmounts.ingredients());

        ImportedRecipe noAmounts = parse("قورمه سبزی\nسبزی، گوشت، لوبیا قرمز، لیمو عمانی و پیاز");
        assertEquals(List.of(ing("سبزی", ""), ing("گوشت", ""), ing("لوبیا قرمز", ""), ing("لیمو عمانی و پیاز", "")),
                noAmounts.ingredients(), "only commas split items");
    }

    @Test
    void listAndMethodOnOneLine() {
        ImportedRecipe r = parse("Omelette\n3 eggs, a little butter, salt. Beat the eggs, cook in butter for 3 minutes.");

        assertEquals(List.of(ing("egg", "3"), ing("butter", "a little"), ing("salt", "")), r.ingredients());
        assertEquals(List.of("Beat the eggs, cook in butter for 3 minutes."), r.steps());
    }

    @Test
    void nameAndListOnOneLineSeparatedByAColon() {
        ImportedRecipe r = parse("کوکو سبزی: سبزی کوکو، تخم مرغ ۴ عدد، گردو، نمک\nهمه را مخلوط کنید و سرخ کنید.");

        assertEquals("کوکو سبزی", r.name());
        assertEquals(List.of(ing("سبزی کوکو", ""), ing("تخم مرغ", "۴ عدد"), ing("گردو", ""), ing("نمک", "")),
                r.ingredients());
        assertEquals(List.of("همه را مخلوط کنید و سرخ کنید."), r.steps());
    }

    @Test
    void methodSentencesWithCommasArentTakenForAList() {
        assertNull(RecipeTextParser.commaListItems("Beat the eggs, cook in butter"));
        assertNull(RecipeTextParser.commaListItems("پیاز را خرد کنید، سرخ کنید"));
        assertNull(RecipeTextParser.commaListItems("just one item"));
        assertEquals(List.of("salt", "pepper"), RecipeTextParser.commaListItems("salt, and pepper."));
    }

    @Test
    void textWithoutARecipeIsRejected() {
        assertTrue(RecipeTextParser.parse("hey how are you\nsee you tomorrow at 5", null, KNOWN).isEmpty());
        assertTrue(RecipeTextParser.parse("Ingredients\nInstructions\nStir.", null, KNOWN).isEmpty(),
                "no ingredients");
        assertTrue(RecipeTextParser.parse("", null, KNOWN).isEmpty());
    }

    @Test
    void ingredientLinesInManyShapes() {
        assertEquals(ing("egg", "2"), RecipeTextParser.parseIngredient("• 2 eggs", KNOWN));
        assertEquals(ing("garlic", "3 cloves"), RecipeTextParser.parseIngredient("3 cloves garlic, minced", KNOWN));
        assertEquals(ing("sugar", "200g"), RecipeTextParser.parseIngredient("200g sugar", KNOWN));
        assertEquals(ing("salt", "a pinch"), RecipeTextParser.parseIngredient("a pinch of salt", KNOWN));
        assertEquals(ing("apple", "1"), RecipeTextParser.parseIngredient("an apple", KNOWN));
        assertEquals(ing("apple", ""), RecipeTextParser.parseIngredient("apples", KNOWN), "'a' isn't read from 'apples'");
        assertEquals(ing("flour", "200 g"), RecipeTextParser.parseIngredient("Flour - 200 g", KNOWN));
        assertEquals(ing("cooked rice", "2 cups"), RecipeTextParser.parseIngredient("2 cups cooked rice", KNOWN));
        assertEquals(ing("tomato", "2"), RecipeTextParser.parseIngredient("1. 2 ripe tomatoes (about 300 g)", KNOWN));
        assertEquals(ing("سیر", "۲ حبه"), RecipeTextParser.parseIngredient("۲ حبه سیر", KNOWN));
        assertEquals(ing("دانه", ""), RecipeTextParser.parseIngredient("دانه", KNOWN), "'نه' (nine) isn't split off");
        assertEquals(ing("پیاز", "۱ عدد"), RecipeTextParser.parseIngredient("پیاز رنده شده ۱ عدد", KNOWN));
        assertNull(RecipeTextParser.parseIngredient("For the sauce:", KNOWN));
    }

    @Test
    void timesInBothLanguages() {
        assertEquals(90, RecipeTextParser.minutesIn("Total time: 1 hour 30 minutes"));
        assertEquals(90, RecipeTextParser.minutesIn("1½ hrs"));
        assertEquals(25, RecipeTextParser.minutesIn("20-25 mins"));
        assertEquals(30, RecipeTextParser.minutesIn("زمان: نیم ساعت"));
        assertEquals(90, RecipeTextParser.minutesIn("۱ ساعت و نیم"));
        assertEquals(45, RecipeTextParser.minutesIn("۴۵ دقیقه"));
        assertEquals(0, RecipeTextParser.minutesIn("9:41"));
        assertEquals(40, RecipeTextParser.prepTime(List.of("Prep: 10 min", "Total: 40 min", "Cook: 20 min")));
    }

    @Test
    void stepsFromAParagraphSplitBySentence() {
        assertEquals(List.of("Mix it.", "Bake it!", "Serve."),
                RecipeTextParser.parseSteps(List.of("Mix it. Bake", "it! Serve.")));
    }

    @Test
    void singularOnlyTouchesEnglishPlurals() {
        assertEquals("berry", RecipeTextParser.singular("berries"));
        assertEquals("potato", RecipeTextParser.singular("potatoes"));
        assertEquals("green bean", RecipeTextParser.singular("green beans"));
        assertEquals("hummus", RecipeTextParser.singular("hummus"));
        assertEquals("molasses", RecipeTextParser.singular("molasses"));
        assertEquals("olive", RecipeTextParser.singular("olives"));
        assertEquals("loaf", RecipeTextParser.singular("loaves"));
        assertEquals("لوبیا", RecipeTextParser.singular("لوبیا"));
    }

    @Test
    void categoryComesFromTheName() {
        assertEquals("Dessert", CategoryGuesser.guess("Chocolate cake", ""));
        assertEquals("Drink", CategoryGuesser.guess("Mango smoothie", ""));
        assertEquals("Breakfast", CategoryGuesser.guess("املت گوجه", ""));
        assertEquals("MainCourse", CategoryGuesser.guess("زرشک پلو با مرغ", ""));
        assertEquals("Other", CategoryGuesser.guess("Something new", "nothing to go on"));
    }
}
