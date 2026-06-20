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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

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
    // Real-Debrid file id, used to request a transcoded (lower-bitrate) stream of this media.
    val sourceId: String? = null,
)

fun RealDebridDownload.asCastableMedia(): CastableMedia? {
    val contentType = normalizedCastContentType(mimeType, filename, type) ?: return null
    val kind = if (contentType.startsWith("audio/")) CastMediaKind.Audio else CastMediaKind.Video
    return CastableMedia(
        title = filename.ifBlank { download.substringAfterLast('/').substringBefore('?').ifBlank { "Debbie media" } },
        url = download,
        contentType = contentType,
        kind = kind,
        sourceId = id,
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

// Formats the Default Media Receiver (cast app id CC1AD845) reliably plays.
// Conservative: anything not listed here (mkv, avi, mov, mpeg, ...) is treated as
// unsupported so we message the user instead of triggering a silent black screen.
fun isChromecastSupportedContentType(contentType: String): Boolean {
    return when (contentType.substringBefore(';').trim().lowercase()) {
        "video/mp4",
        "video/webm",
        "video/3gpp",
        "audio/mp4",
        "audio/mpeg",
        "audio/aac",
        "audio/wav",
        -> true
        else -> false
    }
}

private fun castFormatLabel(media: CastableMedia): String {
    val ext = media.url.substringBefore('?').substringAfterLast('.', "").uppercase()
    if (ext.isNotBlank() && ext.length <= 5) return ext
    return media.contentType.substringAfterLast('/').removePrefix("x-").uppercase()
}

fun castToChromecast(context: Context, media: CastableMedia): String {
    if (!isChromecastSupportedContentType(media.contentType)) {
        return "This Chromecast can't play ${castFormatLabel(media)} files. Try the mp4 alternative or DLNA."
    }

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

// A DLNA renderer we discovered ourselves: enough to display it and POST to it.
data class DlnaDevice(
    val friendlyName: String,
    val controlUrl: String,
    val host: String,
)

private const val AV_TRANSPORT = "urn:schemas-upnp-org:service:AVTransport:1"

// We replaced the upnpcast library here: it never discovered renderers on multi-interface
// phones. This does the same SSDP M-SEARCH a desktop tool does, but pins the socket to the
// Wi-Fi source address so the request actually leaves via wlan0 and the unicast replies
// (which carry each device's description URL) come back to us.
suspend fun searchDlnaDevices(context: Context): List<DlnaDevice> = withContext(Dispatchers.IO) {
    val app = context.applicationContext
    val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    val wifiNetwork = cm.allNetworks.firstOrNull { network ->
        cm.getNetworkCapabilities(network)?.let {
            it.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) &&
                !it.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)
        } == true
    }
    val wifiAddress = wifiNetwork
        ?.let { cm.getLinkProperties(it)?.linkAddresses }
        ?.firstOrNull { it.address is java.net.Inet4Address }
        ?.address

    val locations = ssdpDiscover(wifiAddress)
    val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()
    locations.mapNotNull { describeRenderer(client, it) }.distinctBy { it.controlUrl }
}

// Send M-SEARCH for media renderers and collect the LOCATION header from each reply.
private fun ssdpDiscover(source: java.net.InetAddress?): Set<String> {
    val group = java.net.InetAddress.getByName("239.255.255.250")
    val request = (
        "M-SEARCH * HTTP/1.1\r\n" +
            "HOST: 239.255.255.250:1900\r\n" +
            "MAN: \"ssdp:discover\"\r\n" +
            "MX: 3\r\n" +
            "ST: $AV_TRANSPORT\r\n\r\n"
        ).toByteArray()
    val socket = if (source != null) {
        java.net.DatagramSocket(java.net.InetSocketAddress(source, 0))
    } else {
        java.net.DatagramSocket()
    }
    val locations = linkedSetOf<String>()
    try {
        socket.soTimeout = 1_000
        socket.send(java.net.DatagramPacket(request, request.size, group, 1900))
        val deadline = System.currentTimeMillis() + 4_000
        val buffer = ByteArray(2048)
        while (System.currentTimeMillis() < deadline) {
            val packet = java.net.DatagramPacket(buffer, buffer.size)
            try {
                socket.receive(packet)
            } catch (_: java.net.SocketTimeoutException) {
                continue
            }
            val text = String(packet.data, 0, packet.length)
            text.lineSequence()
                .firstOrNull { it.startsWith("LOCATION:", ignoreCase = true) }
                ?.substringAfter(':')?.trim()
                ?.let { locations.add(it) }
        }
    } finally {
        socket.close()
    }
    return locations
}

// Fetch a device's description XML and pull out its name and AVTransport control URL.
private fun describeRenderer(client: OkHttpClient, location: String): DlnaDevice? {
    val xml = runCatching {
        client.newCall(okhttp3.Request.Builder().url(location).build()).execute().use { response ->
            if (!response.isSuccessful) return null
            response.body?.string() ?: return null
        }
    }.getOrNull() ?: return null

    val controlUrl = avTransportControlUrl(xml)?.let { resolveUrl(location, it) } ?: return null
    val friendlyName = Regex("<friendlyName>(.*?)</friendlyName>", RegexOption.DOT_MATCHES_ALL)
        .find(xml)?.groupValues?.get(1)?.trim().orEmpty()
    val host = runCatching { java.net.URL(location).host }.getOrNull().orEmpty()
    return DlnaDevice(friendlyName.ifBlank { host.ifBlank { "DLNA device" } }, controlUrl, host)
}

// Walk <service> entries; return the controlURL of the one whose serviceType is AVTransport.
private fun avTransportControlUrl(xml: String): String? {
    val services = Regex("<service>(.*?)</service>", RegexOption.DOT_MATCHES_ALL).findAll(xml)
    for (match in services) {
        val block = match.groupValues[1]
        if (block.contains("AVTransport", ignoreCase = true)) {
            return Regex("<controlURL>(.*?)</controlURL>", RegexOption.DOT_MATCHES_ALL)
                .find(block)?.groupValues?.get(1)?.trim()
        }
    }
    return null
}

private fun resolveUrl(base: String, ref: String): String =
    runCatching { java.net.URL(java.net.URL(base), ref).toString() }.getOrDefault(ref)

suspend fun castToDlnaDevice(device: DlnaDevice, media: CastableMedia): String = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
    val setUri = soapAction(
        client, device.controlUrl, "SetAVTransportURI",
        "<InstanceID>0</InstanceID>" +
            "<CurrentURI>${xmlEscape(media.url)}</CurrentURI>" +
            "<CurrentURIMetaData>${xmlEscape(didlMetadata(media))}</CurrentURIMetaData>",
    )
    if (!setUri) return@withContext "Could not start DLNA playback."
    soapAction(client, device.controlUrl, "Play", "<InstanceID>0</InstanceID><Speed>1</Speed>")
    "Casting to ${device.friendlyName}."
}

// POST one AVTransport SOAP action. Returns whether the renderer accepted it (2xx).
private fun soapAction(client: OkHttpClient, controlUrl: String, action: String, inner: String): Boolean {
    val envelope = """<?xml version="1.0" encoding="utf-8"?>""" +
        """<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" """ +
        """s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/"><s:Body>""" +
        """<u:$action xmlns:u="$AV_TRANSPORT">$inner</u:$action></s:Body></s:Envelope>"""
    val request = okhttp3.Request.Builder()
        .url(controlUrl)
        .addHeader("SOAPAction", "\"$AV_TRANSPORT#$action\"")
        .post(envelope.toRequestBody("text/xml; charset=\"utf-8\"".toMediaType()))
        .build()
    return runCatching {
        client.newCall(request).execute().use { response -> response.isSuccessful }
    }.getOrDefault(false)
}

// Minimal DIDL-Lite; some renderers (incl. Sony) refuse a bare URL without metadata.
private fun didlMetadata(media: CastableMedia): String {
    val upnpClass = if (media.kind == CastMediaKind.Audio) "object.item.audioItem.musicTrack" else "object.item.videoItem"
    return """<DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" """ +
        """xmlns:dc="http://purl.org/dc/elements/1.1/" """ +
        """xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/">""" +
        """<item id="0" parentID="-1" restricted="1">""" +
        """<dc:title>${xmlEscape(media.title)}</dc:title>""" +
        """<upnp:class>$upnpClass</upnp:class>""" +
        """<res protocolInfo="http-get:*:${media.contentType}:*">${xmlEscape(media.url)}</res>""" +
        """</item></DIDL-Lite>"""
}

private fun xmlEscape(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&apos;")

fun dlnaDeviceLabel(device: DlnaDevice): String =
    listOf(device.friendlyName, "(${device.host})").joinToString(" ")
