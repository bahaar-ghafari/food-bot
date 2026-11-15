package com.foodbot.bot;

import com.foodbot.lang.Lang;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeployInfoTest {
    private static final Instant STARTED = Instant.parse("2026-11-15T20:00:00Z");

    @Test
    void showsDeployTimeInTehranAndUtcWithTheCommit() {
        String text = DeployInfo.text(Lang.EN, "48be813", "2026-11-15T19:49:13Z", STARTED);

        assertTrue(text.contains("2026-11-15 23:19 Tehran · 19:49 UTC"), text);
        assertTrue(text.contains("48be813"), text);
        assertTrue(text.contains("2026-11-15 23:30 Tehran · 20:00 UTC"), text);
    }

    @Test
    void missingValuesShowAsUnknown() {
        String text = DeployInfo.text(Lang.FA, null, " ", STARTED);
        assertEquals(2, text.split("نامشخص", -1).length - 1, text);
    }
}
