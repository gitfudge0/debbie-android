package dev.gitfudge.debbie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CastingTest {
    @Test
    fun detectsVideoAndAudioFromMimeType() {
        assertEquals("video/mp4", normalizedCastContentType("video/mp4", "file.bin"))
        assertEquals("audio/flac", normalizedCastContentType("audio/flac; charset=utf-8", "file.bin"))
    }

    @Test
    fun detectsVideoAndAudioFromFilenameWhenMimeTypeIsMissing() {
        assertEquals("video/x-matroska", normalizedCastContentType(null, "Movie.Name.mkv"))
        assertEquals("audio/mpeg", normalizedCastContentType(null, "album-track.MP3"))
    }

    @Test
    fun ignoresNonMediaDownloads() {
        assertEquals(null, normalizedCastContentType("application/zip", "archive.zip"))
        assertFalse(isCastableMedia(download("archive.zip", "application/zip")))
    }

    @Test
    fun mapsRealDebridDownloadToCastableMedia() {
        val media = download("episode.webm", null).asCastableMedia()
        assertTrue(media != null)
        assertEquals("episode.webm", media?.title)
        assertEquals(CastMediaKind.Video, media?.kind)
        assertEquals("video/webm", media?.contentType)
    }

    private fun download(filename: String, mimeType: String?) = RealDebridDownload(
        id = "id",
        filename = filename,
        mimeType = mimeType,
        filesize = 1,
        link = "https://example.com/hoster",
        host = "example",
        download = "https://example.com/$filename",
    )
}
