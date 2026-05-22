package dev.gitfudge.debbie

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.documentfile.provider.DocumentFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("debbie")

const val RD_CLIENT_ID = "X245A4XAIBGVM"
private const val DEVICE_GRANT = "http://oauth.net/grant_type/device/1.0"

enum class AuthMethod { OAuth, ApiKey }

@Serializable
data class AuthSession(
    val accessToken: String,
    val refreshToken: String? = null,
    val clientId: String? = null,
    val clientSecret: String? = null,
    val tokenType: String = "Bearer",
    val expiresAtEpochSeconds: Long? = null,
    val method: AuthMethod,
)

@Serializable
data class RealDebridUser(
    val id: Long,
    val username: String,
    val email: String? = null,
    val points: Int? = null,
    val locale: String? = null,
    val avatar: String? = null,
    val type: String,
    val premium: Long,
    val expiration: String? = null,
)

@Serializable
data class RealDebridTorrent(
    val id: String,
    val filename: String,
    val hash: String,
    val bytes: Long,
    val host: String,
    val split: Int? = null,
    val progress: Double,
    val status: String,
    val added: String,
    val links: List<String> = emptyList(),
    val ended: String? = null,
    val speed: Long? = null,
    val seeders: Int? = null,
)

@Serializable
data class RealDebridTorrentFile(
    val id: Int,
    val path: String,
    val bytes: Long,
    val selected: Int,
)

@Serializable
data class RealDebridTorrentInfo(
    val id: String,
    val filename: String,
    val hash: String,
    val bytes: Long,
    val host: String,
    val split: Int? = null,
    val progress: Double,
    val status: String,
    val added: String,
    val links: List<String> = emptyList(),
    val ended: String? = null,
    val speed: Long? = null,
    val seeders: Int? = null,
    @SerialName("original_filename") val originalFilename: String? = null,
    @SerialName("original_bytes") val originalBytes: Long? = null,
    val files: List<RealDebridTorrentFile> = emptyList(),
) {
    fun asTorrent() = RealDebridTorrent(id, filename, hash, bytes, host, split, progress, status, added, links, ended, speed, seeders)
}

@Serializable
data class AddTorrentResponse(val id: String, val uri: String)

@Serializable
data class RealDebridDownload(
    val id: String,
    val filename: String,
    val mimeType: String? = null,
    val filesize: Long,
    val link: String,
    val host: String,
    val chunks: Int? = null,
    val download: String,
    val generated: String? = null,
    val type: String? = null,
)

@Serializable
data class RealDebridAlternativeLink(
    val id: String,
    val filename: String,
    val download: String,
    val type: String? = null,
)

@Serializable
data class RealDebridUnrestrictedLink(
    val id: String,
    val filename: String,
    val mimeType: String? = null,
    val filesize: Long = 0,
    val link: String,
    val host: String,
    val chunks: Int? = null,
    val crc: Int? = null,
    val download: String,
    val streamable: Int? = null,
    val type: String? = null,
    val alternative: List<RealDebridAlternativeLink> = emptyList(),
)

@Serializable
data class DeviceCodeResponse(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    val interval: Int,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("verification_url") val verificationUrl: String,
    @SerialName("direct_verification_url") val directVerificationUrl: String? = null,
)

@Serializable
data class DeviceCredentialsResponse(
    @SerialName("client_id") val clientId: String,
    @SerialName("client_secret") val clientSecret: String,
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("token_type") val tokenType: String,
    @SerialName("refresh_token") val refreshToken: String,
)

interface RealDebridApi {
    @GET("user")
    suspend fun getUser(@Header("Authorization") auth: String): RealDebridUser

    @GET("torrents")
    suspend fun listTorrents(@Header("Authorization") auth: String, @Query("limit") limit: Int = 100): List<RealDebridTorrent>

    @GET("torrents/info/{id}")
    suspend fun getTorrentInfo(@Header("Authorization") auth: String, @Path("id") id: String): RealDebridTorrentInfo

    @FormUrlEncoded
    @POST("torrents/addMagnet")
    suspend fun addMagnet(@Header("Authorization") auth: String, @Field("magnet") magnet: String): AddTorrentResponse

    @Multipart
    @PUT("torrents/addTorrent")
    suspend fun addTorrentFile(@Header("Authorization") auth: String, @Part file: MultipartBody.Part): AddTorrentResponse

    @FormUrlEncoded
    @POST("torrents/selectFiles/{id}")
    suspend fun selectTorrentFiles(@Header("Authorization") auth: String, @Path("id") id: String, @Field("files") files: String): Response<Unit>

    @DELETE("torrents/delete/{id}")
    suspend fun deleteTorrent(@Header("Authorization") auth: String, @Path("id") id: String): Response<Unit>

    @FormUrlEncoded
    @POST("unrestrict/link")
    suspend fun unrestrictLink(@Header("Authorization") auth: String, @Field("link") link: String): RealDebridUnrestrictedLink
}

interface RealDebridOAuthApi {
    @GET("device/code")
    suspend fun requestDeviceCode(@Query("client_id") clientId: String, @Query("new_credentials") newCredentials: String = "yes"): DeviceCodeResponse

    @GET("device/credentials")
    suspend fun requestDeviceCredentials(@Query("client_id") clientId: String, @Query("code") deviceCode: String): Response<DeviceCredentialsResponse>

    @FormUrlEncoded
    @POST("token")
    suspend fun pollDeviceToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") deviceCode: String,
        @Field("grant_type") grantType: String = DEVICE_GRANT,
    ): Response<TokenResponse>

    @FormUrlEncoded
    @POST("token")
    suspend fun refreshAccessToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = "refresh_token",
    ): TokenResponse
}

sealed interface DeviceLoginPoll {
    data class Authorized(val session: AuthSession) : DeviceLoginPoll
    data object Pending : DeviceLoginPoll
    data object Expired : DeviceLoginPoll
    data object SlowDown : DeviceLoginPoll
}

@Singleton
class SecureSessionStore @Inject constructor(@ApplicationContext context: Context, private val json: Json) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "debbie_secure_session",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun read(): AuthSession? = prefs.getString("session", null)?.let { json.decodeFromString<AuthSession>(it) }

    fun write(session: AuthSession) {
        prefs.edit { putString("session", json.encodeToString(AuthSession.serializer(), session)) }
    }

    fun clear() {
        prefs.edit { clear() }
    }
}

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.dataStore
    private val darkThemeKey = booleanPreferencesKey("dark_theme")
    private val autoUnrestrictKey = booleanPreferencesKey("auto_unrestrict")

    val darkTheme: Flow<Boolean> = dataStore.data.map { it[darkThemeKey] ?: false }
    val autoUnrestrict: Flow<Boolean> = dataStore.data.map { it[autoUnrestrictKey] ?: false }

    suspend fun setDarkTheme(enabled: Boolean) = dataStore.edit { it[darkThemeKey] = enabled }
    suspend fun setAutoUnrestrict(enabled: Boolean) = dataStore.edit { it[autoUnrestrictKey] = enabled }
}

@Singleton
class AuthRepository @Inject constructor(
    private val api: RealDebridApi,
    private val oauthApi: RealDebridOAuthApi,
    private val store: SecureSessionStore,
) {
    private val _session = kotlinx.coroutines.flow.MutableStateFlow(store.read())
    val session: Flow<AuthSession?> = _session

    suspend fun startDeviceLogin(): DeviceCodeResponse = oauthApi.requestDeviceCode(RD_CLIENT_ID)

    suspend fun pollDeviceLogin(deviceCode: String): DeviceLoginPoll {
        val credentialsResponse = oauthApi.requestDeviceCredentials(RD_CLIENT_ID, deviceCode)
        if (credentialsResponse.code() == 403) return DeviceLoginPoll.Pending
        if (!credentialsResponse.isSuccessful) return DeviceLoginPoll.Expired
        val credentials = credentialsResponse.body() ?: return DeviceLoginPoll.Pending
        val tokenResponse = oauthApi.pollDeviceToken(credentials.clientId, credentials.clientSecret, deviceCode)
        if (tokenResponse.isSuccessful) {
            val token = tokenResponse.body() ?: return DeviceLoginPoll.Pending
            val session = token.toSession(AuthMethod.OAuth, credentials.clientId, credentials.clientSecret)
            save(session)
            return DeviceLoginPoll.Authorized(session)
        }
        val error = tokenResponse.errorBody()?.string().orEmpty()
        return when {
            "authorization_pending" in error -> DeviceLoginPoll.Pending
            "slow_down" in error -> DeviceLoginPoll.SlowDown
            "expired_token" in error -> DeviceLoginPoll.Expired
            else -> DeviceLoginPoll.Pending
        }
    }

    suspend fun loginWithApiKey(apiKey: String) {
        val session = AuthSession(accessToken = apiKey.trim(), method = AuthMethod.ApiKey)
        api.getUser(session.authHeader())
        save(session)
    }

    suspend fun refreshIfNeeded(): AuthSession? {
        val current = _session.value ?: return null
        if (!current.shouldRefresh()) return current
        val clientId = current.clientId ?: return current
        val clientSecret = current.clientSecret ?: return current
        val refreshToken = current.refreshToken ?: return current
        val refreshed = oauthApi.refreshAccessToken(clientId, clientSecret, refreshToken).toSession(AuthMethod.OAuth, clientId, clientSecret)
        save(refreshed)
        return refreshed
    }

    fun logout() {
        store.clear()
        _session.value = null
    }

    private fun save(session: AuthSession) {
        store.write(session)
        _session.value = session
    }
}

@Singleton
class DebbieRepository @Inject constructor(
    private val api: RealDebridApi,
    private val auth: AuthRepository,
    @ApplicationContext private val context: Context,
) {
    private var cachedUser: RealDebridUser? = null
    private var cachedTorrents: List<RealDebridTorrent> = emptyList()

    suspend fun user(): RealDebridUser = guarded {
        api.getUser(requireSession().authHeader()).also { cachedUser = it }
    } ?: cachedUser ?: throw IllegalStateException("No cached user")

    suspend fun torrents(force: Boolean = false): List<RealDebridTorrent> = guarded {
        if (force || cachedTorrents.isEmpty()) cachedTorrents = api.listTorrents(requireSession().authHeader())
        cachedTorrents
    } ?: cachedTorrents

    suspend fun torrentInfo(id: String): RealDebridTorrentInfo = guarded {
        api.getTorrentInfo(requireSession().authHeader(), id)
    } ?: throw IllegalStateException("Torrent detail is unavailable offline")

    suspend fun addMagnet(magnet: String): AddTorrentResponse {
        require(validateMagnet(magnet)) { "Paste a valid magnet link." }
        return api.addMagnet(requireSession().authHeader(), magnet.trim()).also { cachedTorrents = emptyList() }
    }

    suspend fun addTorrentFile(uri: Uri): AddTorrentResponse {
        val temp = File.createTempFile("debbie-upload", ".torrent", context.cacheDir)
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Could not open torrent file." }
            temp.outputStream().use { output -> input.copyTo(output) }
        }
        val displayName = DocumentFile.fromSingleUri(context, uri)?.name ?: "upload.torrent"
        val body = temp.asRequestBody("application/x-bittorrent".toMediaType())
        val part = MultipartBody.Part.createFormData("file", displayName, body)
        return api.addTorrentFile(requireSession().authHeader(), part).also {
            temp.delete()
            cachedTorrents = emptyList()
        }
    }

    suspend fun selectTorrentFiles(id: String, files: String) {
        api.selectTorrentFiles(requireSession().authHeader(), id, files)
    }

    suspend fun deleteTorrent(id: String) {
        api.deleteTorrent(requireSession().authHeader(), id)
        cachedTorrents = emptyList()
    }

    // Unrestrict each hoster link into one or more direct downloads, keeping the
    // filename / size / host the API hands back so the detail screen can render a
    // real readout instead of a bare URL. Alternatives inherit the parent's host.
    suspend fun unrestrictLinks(links: List<String>): List<RealDebridDownload> {
        val generated = links.flatMap { link ->
            val unrestricted = api.unrestrictLink(requireSession().authHeader(), link)
            buildList {
                add(
                    RealDebridDownload(
                        id = unrestricted.id,
                        filename = unrestricted.filename,
                        mimeType = unrestricted.mimeType,
                        filesize = unrestricted.filesize,
                        link = unrestricted.link,
                        host = unrestricted.host,
                        chunks = unrestricted.chunks,
                        download = unrestricted.download,
                        type = unrestricted.type,
                    ),
                )
                unrestricted.alternative.forEach { alt ->
                    add(
                        RealDebridDownload(
                            id = alt.id,
                            filename = alt.filename,
                            filesize = 0,
                            link = unrestricted.link,
                            host = unrestricted.host,
                            download = alt.download,
                            type = alt.type,
                        ),
                    )
                }
            }
        }
        return generated.filter { it.download.isNotBlank() }.distinctBy { it.download }
    }

    private suspend fun requireSession(): AuthSession = auth.refreshIfNeeded() ?: throw IllegalStateException("Reconnect to Real-Debrid.")

    private suspend fun <T> guarded(block: suspend () -> T): T? = try {
        block()
    } catch (error: retrofit2.HttpException) {
        if (error.code() == 401) auth.logout()
        null
    }
}

fun AuthSession.authHeader(): String = "$tokenType $accessToken"

fun AuthSession.shouldRefresh(nowEpochSeconds: Long = Instant.now().epochSecond): Boolean {
    if (method != AuthMethod.OAuth) return false
    val expiresAt = expiresAtEpochSeconds ?: return false
    return expiresAt - nowEpochSeconds < 300
}

private fun TokenResponse.toSession(method: AuthMethod, clientId: String?, clientSecret: String?) = AuthSession(
    accessToken = accessToken,
    refreshToken = refreshToken,
    clientId = clientId,
    clientSecret = clientSecret,
    tokenType = tokenType,
    expiresAtEpochSeconds = Instant.now().epochSecond + expiresIn,
    method = method,
)

enum class StatusTone { Sap, Amber, Signal, Mute }

fun statusTone(status: String): StatusTone = when (status) {
    "downloaded", "ready" -> StatusTone.Sap
    "uploading", "downloading" -> StatusTone.Amber
    "magnet_error", "virus", "dead", "error" -> StatusTone.Signal
    else -> StatusTone.Mute
}

fun statusLabel(status: String): String = when (status) {
    "downloaded", "ready" -> "Ready"
    "uploading" -> "Uploading"
    "downloading" -> "Downloading"
    "queued" -> "Queued"
    "waiting_files_selection" -> "Needs files"
    "magnet_resolved" -> "Resolving"
    "magnet_error" -> "Magnet error"
    "virus" -> "Virus detected"
    "dead" -> "Dead"
    "error" -> "Error"
    else -> status
}

fun isActive(status: String) = status == "downloading" || status == "uploading"
fun isReady(status: String) = status == "downloaded" || status == "ready"
fun isError(status: String) = status in setOf("magnet_error", "virus", "dead", "error")
fun isNeedsAction(status: String) = status == "waiting_files_selection"
fun validateMagnet(value: String) = value.trim().startsWith("magnet:?xt=urn:btih:", ignoreCase = true)

fun fmtSize(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.2f GB".format(Locale.US, bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> "%.0f MB".format(Locale.US, bytes / 1_048_576.0)
    bytes >= 1_024L -> "%.0f KB".format(Locale.US, bytes / 1_024.0)
    else -> "$bytes B"
}

fun fmtSizeGB(bytes: Long): String = "%.2f GB".format(Locale.US, bytes / 1_073_741_824.0)
fun fmtSpeed(bytesPerSec: Long): String = "%.1f MB/s".format(Locale.US, bytesPerSec / 1_048_576.0)

fun fmtEta(bytes: Long, progress: Double, speedBytesPerSec: Long): String {
    if (speedBytesPerSec <= 0) return "-"
    val remaining = bytes * (1 - progress)
    val mins = kotlin.math.ceil((remaining / speedBytesPerSec) / 60).toInt()
    return if (mins > 60) "${kotlin.math.ceil(mins / 60.0).toInt()}h" else "${mins}m"
}

fun fmtTimeRel(isoString: String, now: Instant = Instant.now()): String {
    val diff = now.toEpochMilli() - Instant.parse(isoString).toEpochMilli()
    val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MINUTES.toHours(mins)
    val days = TimeUnit.HOURS.toDays(hours)
    return when {
        days > 0 -> "${days}d ago"
        hours > 0 -> "${hours}h ago"
        mins > 0 -> "${mins}m ago"
        else -> "just now"
    }
}

fun fmtDate(isoString: String): String = Instant.parse(isoString)
    .atZone(ZoneId.systemDefault())
    .toLocalDate()
    .format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))

fun shortHash(hash: String) = hash.take(8).uppercase(Locale.US)

fun premiumDaysLeft(expirationIso: String?, today: LocalDate = LocalDate.now()): Int {
    if (expirationIso == null) return 0
    val expiration = Instant.parse(expirationIso).atZone(ZoneId.systemDefault()).toLocalDate()
    return kotlin.math.max(0, java.time.temporal.ChronoUnit.DAYS.between(today, expiration).toInt())
}

fun flattenDirectLinks(links: List<String>): List<String> = links.filter { it.isNotBlank() }.distinct()

@Module
@InstallIn(SingletonComponent::class)
object DebbieModule {
    @Provides
    @Singleton
    fun json(): Json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Provides
    @Singleton
    fun okHttp(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Singleton
    fun realDebridApi(json: Json, client: OkHttpClient): RealDebridApi = Retrofit.Builder()
        .baseUrl("https://api.real-debrid.com/rest/1.0/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(RealDebridApi::class.java)

    @Provides
    @Singleton
    fun oauthApi(json: Json, client: OkHttpClient): RealDebridOAuthApi = Retrofit.Builder()
        .baseUrl("https://api.real-debrid.com/oauth/v2/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(RealDebridOAuthApi::class.java)
}
