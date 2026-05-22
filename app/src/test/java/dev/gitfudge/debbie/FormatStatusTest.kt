package dev.gitfudge.debbie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class FormatStatusTest {
    @Test
    fun mapsStatusLabelsAndTones() {
        assertEquals("Ready", statusLabel("downloaded"))
        assertEquals("Needs files", statusLabel("waiting_files_selection"))
        assertEquals(StatusTone.Sap, statusTone("ready"))
        assertEquals(StatusTone.Signal, statusTone("magnet_error"))
        assertTrue(isActive("downloading"))
        assertTrue(isReady("downloaded"))
        assertTrue(isError("dead"))
        assertTrue(isNeedsAction("waiting_files_selection"))
    }

    @Test
    fun formatsSizesSpeedEtaAndRelativeTime() {
        assertEquals("1.00 GB", fmtSize(1_073_741_824))
        assertEquals("512 MB", fmtSize(536_870_912))
        assertEquals("1.0 MB/s", fmtSpeed(1_048_576))
        assertEquals("9m", fmtEta(1_000, 0.5, 1))
        assertEquals("2h ago", fmtTimeRel("2026-05-21T08:00:00Z", Instant.parse("2026-05-21T10:01:00Z")))
    }

    @Test
    fun validatesMagnetsAndFlattensLinks() {
        assertTrue(validateMagnet("magnet:?xt=urn:btih:abc"))
        assertTrue(validateMagnet("MAGNET:?XT=URN:BTIH:abc"))
        assertFalse(validateMagnet("https://example.com/file.torrent"))
        assertEquals(listOf("a", "b"), flattenDirectLinks(listOf("a", "", "b", "a")))
    }

    @Test
    fun handlesPremiumDaysAndRefreshDecision() {
        assertEquals(9, premiumDaysLeft("2026-05-30T00:00:00Z", LocalDate.of(2026, 5, 21)))
        assertTrue(
            AuthSession(
                accessToken = "token",
                refreshToken = "refresh",
                clientId = "client",
                clientSecret = "secret",
                expiresAtEpochSeconds = 1_000,
                method = AuthMethod.OAuth,
            ).shouldRefresh(nowEpochSeconds = 800),
        )
        assertFalse(AuthSession(accessToken = "token", method = AuthMethod.ApiKey).shouldRefresh())
    }
}
