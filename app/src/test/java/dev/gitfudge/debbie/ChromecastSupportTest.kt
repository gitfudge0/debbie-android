package dev.gitfudge.debbie

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChromecastSupportTest {
    @Test
    fun mp4IsSupported() {
        assertTrue(isChromecastSupportedContentType("video/mp4"))
    }

    @Test
    fun mkvAndAviAreUnsupported() {
        assertFalse(isChromecastSupportedContentType("video/x-matroska"))
        assertFalse(isChromecastSupportedContentType("video/x-msvideo"))
    }
}
