package com.foodbot.social;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartnerRepositoryTest {

    @Test
    void linkIsSymmetricAndTheLowerIdOwnsTheList(@TempDir Path dir) {
        PartnerRepository repo = new PartnerRepository(dir.resolve("partners.json"));
        assertTrue(repo.link(9L, 4L));
        assertEquals(4L, repo.partnerOf(9L));
        assertEquals(9L, repo.partnerOf(4L));
        assertEquals(4L, repo.listOwner(9L));
        assertEquals(4L, repo.listOwner(4L));
        assertEquals(7L, repo.listOwner(7L));
    }

    @Test
    void cannotLinkToYourself(@TempDir Path dir) {
        PartnerRepository repo = new PartnerRepository(dir.resolve("partners.json"));
        assertFalse(repo.link(3L, 3L));
        assertNull(repo.partnerOf(3L));
    }

    @Test
    void nicknamesArePerSideAndNeedAPartner(@TempDir Path dir) {
        PartnerRepository repo = new PartnerRepository(dir.resolve("partners.json"));
        repo.setNickname(1L, "ignored");
        assertNull(repo.nicknameOf(1L));
        repo.link(1L, 2L);
        repo.setNickname(1L, "  Sara ");
        assertEquals("Sara", repo.nicknameOf(1L));
        assertNull(repo.nicknameOf(2L));
        repo.setNickname(1L, " ");
        assertNull(repo.nicknameOf(1L));
    }

    @Test
    void unlinkClearsBothSidesAndNicknames(@TempDir Path dir) {
        PartnerRepository repo = new PartnerRepository(dir.resolve("partners.json"));
        repo.link(1L, 2L);
        repo.setNickname(1L, "Sara");
        repo.setNickname(2L, "Ali");
        assertEquals(2L, repo.unlink(1L));
        assertNull(repo.partnerOf(1L));
        assertNull(repo.partnerOf(2L));
        assertNull(repo.nicknameOf(2L));
        assertEquals(1L, repo.listOwner(1L));
        assertNull(repo.unlink(1L));
    }

    @Test
    void relinkingDropsTheOldPartnership(@TempDir Path dir) {
        PartnerRepository repo = new PartnerRepository(dir.resolve("partners.json"));
        repo.link(1L, 2L);
        repo.link(1L, 3L);
        assertNull(repo.partnerOf(2L));
        assertEquals(3L, repo.partnerOf(1L));
    }

    @Test
    void persistsAcrossReload(@TempDir Path dir) {
        Path file = dir.resolve("partners.json");
        PartnerRepository repo = new PartnerRepository(file);
        repo.link(1L, 2L);
        repo.setNickname(2L, "Sara");
        PartnerRepository reloaded = new PartnerRepository(file);
        assertEquals(1L, reloaded.partnerOf(2L));
        assertEquals("Sara", reloaded.nicknameOf(2L));
    }
}
