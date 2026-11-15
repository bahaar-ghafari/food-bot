package com.foodbot.bot;

import com.foodbot.importer.ImportedRecipe;
import com.foodbot.ai.MealDbSuggestionService;
import com.foodbot.importer.RecipeImporter;
import com.foodbot.food.AddFoodSession;
import com.foodbot.food.FavoriteRepository;
import com.foodbot.food.Food;
import com.foodbot.food.FoodRepository;
import com.foodbot.food.ShoppingListRepository;
import com.foodbot.lang.Lang;
import com.foodbot.lang.LanguageRepository;
import com.foodbot.lang.Messages;
import com.foodbot.social.FollowRepository;
import com.foodbot.social.PartnerRepository;
import com.foodbot.social.UsernameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeImportFlowTest {
    private static final long CHAT = 5L;

    private final List<BotApiMethod<?>> sent = new ArrayList<>();
    private final List<Runnable> pendingBackgroundWork = new ArrayList<>();
    private final FakeImporter importer = new FakeImporter();
    private BotContext ctx;
    private int nextMessageId = 100;

    private static final ImportedRecipe OMELETTE = new ImportedRecipe("Omelette", 10, "Breakfast",
            List.of(new ImportedRecipe.Ingredient("EGG", "3"), new ImportedRecipe.Ingredient("butter", ""),
                    new ImportedRecipe.Ingredient("chives", "1 tbsp")),
            List.of("Beat the eggs.", "Melt the butter and cook the eggs until just set."));

    @BeforeEach
    void setUp(@TempDir Path dir) {
        ApiExecutor recorder = new ApiExecutor() {
            @Override
            @SuppressWarnings("unchecked")
            public <T extends Serializable, M extends BotApiMethod<T>> T execute(M method) {
                sent.add(method);
                if (method instanceof SendMessage) {
                    Message message = new Message();
                    message.setMessageId(nextMessageId++);
                    return (T) message;
                }
                return null;
            }

            @Override
            public byte[] downloadFile(String fileId) {
                return ("bytes-of-" + fileId).getBytes();
            }
        };
        ctx = new BotContext(recorder, 1L, 99L,
                new FoodRepository(dir.resolve("foods.json")),
                new LanguageRepository(dir.resolve("languages.json")),
                new FollowRepository(dir.resolve("follows.json")),
                new UsernameRepository(dir.resolve("usernames.json")),
                new FavoriteRepository(dir.resolve("favorites.json")),
                new ShoppingListRepository(dir.resolve("shopping.json")),
                new PartnerRepository(dir.resolve("partners.json")),
                new MealDbSuggestionService(), importer);
        // Hold background work so tests can act while an import is "running", then release it.
        ctx.backgroundExecutor = pendingBackgroundWork::add;
        ctx.languageRepository.set(CHAT, Lang.EN);
        ctx.foodRepository.add(new Food("f1", "Scrambled", 5, "Breakfast", List.of("egg", "milk"), null, 9L,
                "", Lang.EN));
    }

    private void runBackgroundWork() {
        List<Runnable> work = new ArrayList<>(pendingBackgroundWork);
        pendingBackgroundWork.clear();
        work.forEach(Runnable::run);
    }

    private AddFoodSession startAddFood() {
        AddFoodSession session = new AddFoodSession();
        ctx.addFoodSessions.put(CHAT, session);
        ctx.addFood.sendAddFoodNamePrompt(CHAT, session, Lang.EN);
        return session;
    }

    private String lastScreenText() {
        for (int i = sent.size() - 1; i >= 0; i--) {
            if (sent.get(i) instanceof EditMessageText edit) {
                return edit.getText();
            }
            if (sent.get(i) instanceof SendMessage message) {
                return message.getText();
            }
        }
        return null;
    }

    private CallbackQuery callback() {
        Message message = new Message();
        message.setMessageId(42);
        CallbackQuery query = new CallbackQuery();
        query.setId("cb1");
        query.setMessage(message);
        return query;
    }

    @Test
    void namePromptOffersTheShortcutOnlyWhenImportIsAvailable() {
        startAddFood();
        assertEquals(Messages.get(Lang.EN, "addfood.ask_name_import"), lastScreenText());

        importer.enabled = false;
        sent.clear();
        startAddFood();
        assertEquals(Messages.get(Lang.EN, "addfood.ask_name"), lastScreenText());
    }

    @Test
    void pastedRecipeFillsEveryFieldAndLandsOnTheReview() {
        AddFoodSession session = startAddFood();
        importer.result = Optional.of(OMELETTE);

        ctx.addFood.handleAddFoodText(CHAT, "Omelette\nIngredients: 3 eggs, butter, chives\n...", Lang.EN);

        assertEquals(AddFoodSession.Step.IMPORTING, session.getStep());
        assertEquals(Messages.get(Lang.EN, "addfood.import_reading"), lastScreenText());
        assertTrue(sent.stream().anyMatch(DeleteMessage.class::isInstance), "old prompt is replaced below the paste");

        runBackgroundWork();

        assertEquals("Omelette\nIngredients: 3 eggs, butter, chives\n...", importer.lastText);
        assertEquals(AddFoodSession.Step.REVIEW, session.getStep());
        assertTrue(session.isImported());
        assertEquals("Omelette", session.getName());
        assertEquals(10, session.getPrepTimeMinutes());
        assertEquals("Breakfast", session.getCategory());
        // "EGG" reuses the existing spelling; empty amounts aren't stored.
        assertEquals(List.of("egg", "butter", "chives"), new ArrayList<>(session.getSelectedIngredients()));
        assertEquals(Map.of("egg", "3", "chives", "1 tbsp"), session.getIngredientAmounts());
        assertEquals(OMELETTE.steps(), session.getRecipeSteps());
        assertTrue(session.getCandidateIngredients().containsAll(List.of("egg", "milk", "butter", "chives")));
        assertTrue(lastScreenText().startsWith(Messages.get(Lang.EN, "addfood.import_review_header")));
    }

    @Test
    void confirmingAnImportedRecipeSavesIt() {
        AddFoodSession session = startAddFood();
        importer.result = Optional.of(OMELETTE);
        ctx.addFood.handleAddFoodText(CHAT, "a long pasted recipe\nwith several lines", Lang.EN);
        runBackgroundWork();

        ctx.addFood.handleAddFoodConfirmCallback(callback(), CHAT, BotConstants.CB_ADDFOOD_CONFIRM);

        assertFalse(ctx.addFoodSessions.containsKey(CHAT));
        Food saved = ctx.foodRepository.findVisibleTo(CHAT, Lang.EN).stream()
                .filter(f -> f.getName().equals("Omelette")).findFirst().orElseThrow();
        assertEquals(List.of("egg", "butter", "chives"), saved.getIngredients());
        assertNull(session.getPhotoFileId());
    }

    @Test
    void aShortNameIsStillTreatedAsTheName() {
        AddFoodSession session = startAddFood();

        ctx.addFood.handleAddFoodText(CHAT, "Pancakes", Lang.EN);

        assertTrue(pendingBackgroundWork.isEmpty());
        assertEquals("Pancakes", session.getName());
        assertEquals(AddFoodSession.Step.AWAITING_PREP_TIME, session.getStep());
    }

    @Test
    void longTextIsJustANameWhenImportIsUnavailable() {
        importer.enabled = false;
        AddFoodSession session = startAddFood();

        ctx.addFood.handleAddFoodText(CHAT, "Omelette\nwith eggs", Lang.EN);

        assertTrue(pendingBackgroundWork.isEmpty());
        assertEquals(AddFoodSession.Step.AWAITING_PREP_TIME, session.getStep());
    }

    @Test
    void aFailedImportGoesBackToTheNameQuestion() {
        AddFoodSession session = startAddFood();
        importer.result = Optional.empty();

        ctx.addFood.handleAddFoodText(CHAT, "this is not\na recipe at all", Lang.EN);
        runBackgroundWork();

        assertEquals(AddFoodSession.Step.AWAITING_NAME, session.getStep());
        assertTrue(lastScreenText().startsWith(Messages.get(Lang.EN, "addfood.import_failed")));
        assertFalse(session.isImported());
    }

    @Test
    void screenshotAtTheNameStepIsReadWithItsCaption() {
        AddFoodSession session = startAddFood();
        importer.result = Optional.of(OMELETTE);

        assertTrue(ctx.addFood.handleAddFoodImage(CHAT, "PIC1", "image/png", "grandma's version", Lang.EN));
        runBackgroundWork();

        assertEquals("bytes-of-PIC1", new String(importer.lastImage));
        assertEquals("image/png", importer.lastMediaType);
        assertEquals("grandma's version", importer.lastCaption);
        assertEquals(AddFoodSession.Step.REVIEW, session.getStep());
        assertNull(session.getPhotoFileId(), "the screenshot is not used as the dish's photo");
    }

    @Test
    void imageLaterInTheFlowIsNotImported() {
        AddFoodSession session = startAddFood();
        session.setStep(AddFoodSession.Step.AWAITING_PHOTO);

        assertFalse(ctx.addFood.handleAddFoodImage(CHAT, "PIC1", "image/jpeg", null, Lang.EN));
        assertTrue(pendingBackgroundWork.isEmpty());
    }

    @Test
    void inputWhileReadingIsHeldOff() {
        AddFoodSession session = startAddFood();
        ctx.addFood.handleAddFoodText(CHAT, "some\nrecipe", Lang.EN);
        sent.clear();

        ctx.addFood.handleAddFoodText(CHAT, "hello?", Lang.EN);
        ctx.addFood.handleAddFoodPhoto(CHAT, "PIC2", Lang.EN);

        assertEquals(AddFoodSession.Step.IMPORTING, session.getStep());
        assertEquals(2, sent.size());
        sent.forEach(m -> assertEquals(Messages.get(Lang.EN, "addfood.import_busy"), ((SendMessage) m).getText()));
    }

    @Test
    void backWhileReadingAbandonsTheImport() {
        AddFoodSession session = startAddFood();
        importer.result = Optional.of(OMELETTE);
        ctx.addFood.handleAddFoodText(CHAT, "some\nrecipe", Lang.EN);

        ctx.addFood.handleAddFoodBackCallback(callback(), CHAT, BotConstants.CB_ADDFOOD_BACK);
        assertEquals(AddFoodSession.Step.AWAITING_NAME, session.getStep());
        runBackgroundWork();

        assertEquals(AddFoodSession.Step.AWAITING_NAME, session.getStep());
        assertNull(session.getName());
    }

    @Test
    void onlyTheLatestImportAttemptCounts() {
        AddFoodSession session = startAddFood();
        importer.result = Optional.of(OMELETTE);
        ctx.addFood.handleAddFoodText(CHAT, "first\nrecipe", Lang.EN);
        ctx.addFood.handleAddFoodBackCallback(callback(), CHAT, BotConstants.CB_ADDFOOD_BACK);
        ctx.addFood.handleAddFoodText(CHAT, "second\nrecipe", Lang.EN);

        Runnable first = pendingBackgroundWork.remove(0);
        importer.result = Optional.of(new ImportedRecipe("Second", 5, "Snack",
                List.of(new ImportedRecipe.Ingredient("bread", "")), List.of()));
        first.run();
        assertEquals(AddFoodSession.Step.IMPORTING, session.getStep(), "stale result ignored");

        runBackgroundWork();
        assertEquals("Second", session.getName());
    }

    @Test
    void cancelWhileReadingDropsTheResult() {
        startAddFood();
        importer.result = Optional.of(OMELETTE);
        ctx.addFood.handleAddFoodText(CHAT, "some\nrecipe", Lang.EN);

        ctx.addFood.handleAddFoodCancelCallback(callback(), CHAT, BotConstants.CB_ADDFOOD_CANCEL);
        runBackgroundWork();

        assertFalse(ctx.addFoodSessions.containsKey(CHAT));
    }

    @Test
    void pastedRecipeDetectionIsLimitedToLongOrMultiLineText() {
        assertFalse(AddFoodHandler.looksLikePastedRecipe("Ghormeh Sabzi"));
        assertTrue(AddFoodHandler.looksLikePastedRecipe("Pasta\n200 g spaghetti"));
        assertTrue(AddFoodHandler.looksLikePastedRecipe("x".repeat(81)));
    }

    private static final class FakeImporter implements RecipeImporter {
        boolean enabled = true;
        Optional<ImportedRecipe> result = Optional.empty();
        String lastText;
        byte[] lastImage;
        String lastMediaType;
        String lastCaption;

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public Optional<ImportedRecipe> fromText(String text, Lang lang, List<String> knownIngredients) {
            lastText = text;
            return result;
        }

        @Override
        public Optional<ImportedRecipe> fromImage(byte[] image, String mediaType, String caption, Lang lang,
                                                  List<String> knownIngredients) {
            lastImage = image;
            lastMediaType = mediaType;
            lastCaption = caption;
            return result;
        }
    }
}
