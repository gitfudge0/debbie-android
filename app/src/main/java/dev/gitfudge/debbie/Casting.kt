package dev.gitfudge.debbie

import android.content.Context
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.NotificationOptions
import com.google.android.gms.cast.framework.media.widget.ExpandedControllerActivity
import com.yinnho.upnpcast.DLNACast

class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        val notificationOptions = NotificationOptions.Builder()
            .setTargetActivityClassName(CastExpandedControlsActivity::class.java.name)
            .build()
        val mediaOptions = CastMediaOptions.Builder()
            .setNotificationOptions(notificationOptions)
            .setExpandedControllerActivityClassName(CastExpandedControlsActivity::class.java.name)
            .build()
        return CastOptions.Builder()
            .setReceiverApplicationId(context.getString(R.string.cast_app_id))
            .setCastMediaOptions(mediaOptions)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): MutableList<SessionProvider>? = null
}

class CastExpandedControlsActivity : ExpandedControllerActivity()

enum class CastMediaKind { Video, Audio }

data class CastableMedia(
    val title: String,
    val url: String,
    val contentType: String,
    val kind: CastMediaKind,
)

fun RealDebridDownload.asCastableMedia(): CastableMedia? {
    val contentType = normalizedCastContentType(mimeType, filename, type) ?: return null
    val kind = if (contentType.startsWith("audio/")) CastMediaKind.Audio else CastMediaKind.Video
    return CastableMedia(
        title = filename.ifBlank { download.substringAfterLast('/').substringBefore('?').ifBlank { "Debbie media" } },
        url = download,
        contentType = contentType,
        kind = kind,
    )
}

fun isCastableMedia(download: RealDebridDownload): Boolean = download.asCastableMedia() != null

fun normalizedCastContentType(mimeType: String?, filename: String, type: String? = null): String? {
    val explicit = mimeType
        ?.substringBefore(';')
        ?.trim()
        ?.lowercase()
        ?.takeIf { it.startsWith("video/") || it.startsWith("audio/") }
    if (explicit != null) return explicit

    val extension = filename.substringBefore('?').substringAfterLast('.', "").lowercase()
        .ifBlank { type?.lowercase().orEmpty() }
    return when (extension) {
        "mp4", "m4v" -> "video/mp4"
        "mkv" -> "video/x-matroska"
        "webm" -> "video/webm"
        "mov" -> "video/quicktime"
        "avi" -> "video/x-msvideo"
        "mpeg", "mpg" -> "video/mpeg"
        "3gp" -> "video/3gpp"
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "aac" -> "audio/aac"
        "flac" -> "audio/flac"
        "ogg", "oga" -> "audio/ogg"
        "opus" -> "audio/opus"
        "wav" -> "audio/wav"
        else -> null
    }
}

fun castToChromecast(context: Context, media: CastableMedia): String {
    val session = runCatching {
        CastContext.getSharedInstance(context.applicationContext).sessionManager.currentCastSession
    }.getOrElse { return it.message ?: "Could not start Chromecast." }

    val remoteMediaClient = session?.remoteMediaClient ?: return "Choose a Chromecast first."
    val metadata = MediaMetadata(
        if (media.kind == CastMediaKind.Audio) MediaMetadata.MEDIA_TYPE_MUSIC_TRACK else MediaMetadata.MEDIA_TYPE_MOVIE,
    ).apply {
        putString(MediaMetadata.KEY_TITLE, media.title)
    }
    val info = MediaInfo.Builder(media.url)
        .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
        .setContentType(media.contentType)
        .setMetadata(metadata)
        .build()
    remoteMediaClient.load(
        MediaLoadRequestData.Builder()
            .setMediaInfo(info)
            .setAutoplay(true)
            .build(),
    )
    return "Casting to Chromecast."
}

suspend fun searchDlnaDevices(context: Context): List<Any> {
    DLNACast.init(context.applicationContext)
    return DLNACast.search(timeout = 5_000)
}

suspend fun castToDlnaDevice(device: Any, media: CastableMedia): String {
    val success = DLNACast.castToDevice(device as DLNACast.Device, media.url, media.title)
    return if (success) "Casting to DLNA device." else "Could not start DLNA playback."
}

fun dlnaDeviceLabel(device: Any): String {
    val name = runCatching { device.javaClass.getMethod("getName").invoke(device) as? String }.getOrNull()
    val address = runCatching { device.javaClass.getMethod("getAddress").invoke(device) as? String }.getOrNull()
    return listOfNotNull(name, address?.let { "($it)" }).joinToString(" ").ifBlank { "DLNA device" }
}
