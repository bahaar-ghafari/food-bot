package com.foodbot.bot;

import com.foodbot.lang.Lang;
import com.foodbot.lang.Messages;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * What's deployed, for the superadmin's "Deploy info" button. The deploy workflow writes DEPLOY_COMMIT and
 * DEPLOYED_AT into deploy-info.env on the server on every push to main, and compose passes them in.
 */
final class DeployInfo {
    private static final ZoneId TEHRAN = ZoneId.of("Asia/Tehran");
    private static final DateTimeFormatter LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("UTC"));

    private DeployInfo() {
    }

    static String text(Lang lang) {
        return text(lang, System.getenv("DEPLOY_COMMIT"), System.getenv("DEPLOYED_AT"),
                Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().getStartTime()));
    }

    static String text(Lang lang, String commit, String deployedAt, Instant startedAt) {
        String deployed = Messages.get(lang, "deploy.unknown");
        if (deployedAt != null && !deployedAt.isBlank()) {
            try {
                deployed = format(Instant.parse(deployedAt.trim()));
            } catch (DateTimeParseException e) {
                deployed = deployedAt.trim();
            }
        }
        String commitText = commit != null && !commit.isBlank() ? commit.trim() : Messages.get(lang, "deploy.unknown");
        return Messages.get(lang, "deploy.info", deployed, commitText, format(startedAt));
    }

    /** Tehran time first (where the admin is), with UTC alongside for matching against GitHub. */
    static String format(Instant instant) {
        return LOCAL.format(instant.atZone(TEHRAN)) + " Tehran · " + UTC.format(instant) + " UTC";
    }
}
