package dev.gitfudge.debbie

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppSettings(val darkTheme: Boolean = false, val autoUnrestrict: Boolean = false)

@HiltViewModel
class SessionViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val session = authRepository.session.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val settings = combine(settingsRepository.darkTheme, settingsRepository.autoUnrestrict) { dark, auto ->
        AppSettings(dark, auto)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setDarkTheme(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setDarkTheme(enabled)
    }
}

data class LoginState(
    val mode: LoginMode = LoginMode.OAuth,
    val apiKey: String = "",
    val device: DeviceCodeResponse? = null,
    val busy: Boolean = false,
    val message: String? = null,
)

enum class LoginMode { OAuth, ApiKey }

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    val state = MutableStateFlow(LoginState())
    val darkTheme = settings.darkTheme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    private var pollJob: Job? = null

    fun setMode(mode: LoginMode) {
        state.value = state.value.copy(mode = mode, message = null)
    }

    fun setApiKey(value: String) {
        state.value = state.value.copy(apiKey = value)
    }

    fun setDarkTheme(enabled: Boolean) = viewModelScope.launch {
        settings.setDarkTheme(enabled)
    }

    fun loginWithApiKey() = viewModelScope.launch {
        runCatching {
            state.value = state.value.copy(busy = true, message = null)
            auth.loginWithApiKey(state.value.apiKey)
        }.onFailure {
            state.value = state.value.copy(busy = false, message = it.message ?: "Real-Debrid rejected that token.")
        }
    }

    fun startDeviceLogin() = viewModelScope.launch {
        runCatching {
            state.value = state.value.copy(busy = true, message = "Requesting device code.")
            auth.startDeviceLogin()
        }.onSuccess { device ->
            state.value = state.value.copy(device = device, busy = false, message = "Authorize Debbie in Real-Debrid.")
            poll(device)
        }.onFailure {
            state.value = state.value.copy(busy = false, message = it.message ?: "Could not start device login.")
        }
    }

    private fun poll(device: DeviceCodeResponse) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            var interval = device.interval.coerceAtLeast(3)
            while (true) {
                delay(interval * 1_000L)
                when (val result = auth.pollDeviceLogin(device.deviceCode)) {
                    is DeviceLoginPoll.Authorized -> return@launch
                    DeviceLoginPoll.Pending -> state.value = state.value.copy(message = "Waiting for authorization.")
                    DeviceLoginPoll.SlowDown -> {
                        interval += 3
                        state.value = state.value.copy(message = "Real-Debrid asked Debbie to slow down.")
                    }
                    DeviceLoginPoll.Expired -> {
                        state.value = state.value.copy(device = null, message = "Device code expired.")
                        return@launch
                    }
                }
            }
        }
    }
}

data class DashboardState(
    val loading: Boolean = true,
    val user: RealDebridUser? = null,
    val torrents: List<RealDebridTorrent> = emptyList(),
    val error: String? = null,
    val showAdd: Boolean = false,
    val magnet: String = "",
    val message: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(private val repository: DebbieRepository) : ViewModel() {
    val state = MutableStateFlow(DashboardState())

    init {
        refresh()
        viewModelScope.launch {
            while (true) {
                delay(4_000)
                refresh(silent = true)
            }
        }
    }

    fun showAdd(show: Boolean) {
        state.value = state.value.copy(showAdd = show, message = null)
    }

    fun setMagnet(value: String) {
        state.value = state.value.copy(magnet = value)
    }

    fun addMagnet() = viewModelScope.launch {
        runCatching {
            repository.addMagnet(state.value.magnet)
            state.value = state.value.copy(showAdd = false, magnet = "", message = "Torrent added.")
            refresh()
        }.onFailure {
            state.value = state.value.copy(message = it.message ?: "Could not add magnet.")
        }
    }

    fun addTorrentFile(uri: Uri) = viewModelScope.launch {
        runCatching {
            repository.addTorrentFile(uri)
            state.value = state.value.copy(showAdd = false, message = "Torrent file uploaded.")
            refresh()
        }.onFailure {
            state.value = state.value.copy(message = it.message ?: "Could not upload torrent file.")
        }
    }

    fun refresh(silent: Boolean = false) = viewModelScope.launch {
        runCatching {
            if (!silent) state.value = state.value.copy(loading = true, error = null)
            val user = repository.user()
            val torrents = repository.torrents(force = true)
            state.value = state.value.copy(loading = false, user = user, torrents = torrents, error = null)
        }.onFailure {
            state.value = state.value.copy(loading = false, error = it.message ?: "Could not load dashboard.")
        }
    }
}

data class DetailState(
    val loading: Boolean = true,
    val torrent: RealDebridTorrentInfo? = null,
    val selectedFiles: Set<Int> = emptySet(),
    val directLinks: List<RealDebridDownload> = emptyList(),
    val generating: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: DebbieRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val id: String = checkNotNull(savedStateHandle["id"])
    val state = MutableStateFlow(DetailState())

    init {
        refresh()
        viewModelScope.launch {
            while (true) {
                delay(4_000)
                refresh(silent = true)
            }
        }
    }

    fun refresh(silent: Boolean = false) = viewModelScope.launch {
        runCatching {
            if (!silent) state.value = state.value.copy(loading = true, message = null)
            val torrent = repository.torrentInfo(id)
            val selected = torrent.files.filter { it.selected == 1 }.map { it.id }.toSet()
            state.value = state.value.copy(loading = false, torrent = torrent, selectedFiles = if (state.value.selectedFiles.isEmpty()) selected else state.value.selectedFiles)
        }.onFailure {
            state.value = state.value.copy(loading = false, message = it.message ?: "Could not load torrent.")
        }
    }

    fun toggleFile(id: Int) {
        val current = state.value.selectedFiles
        state.value = state.value.copy(selectedFiles = if (id in current) current - id else current + id)
    }

    fun selectAll() {
        state.value.torrent?.let { state.value = state.value.copy(selectedFiles = it.files.map { file -> file.id }.toSet()) }
    }

    fun selectNone() {
        state.value = state.value.copy(selectedFiles = emptySet())
    }

    fun submitFileSelection() = viewModelScope.launch {
        val files = state.value.selectedFiles.sorted().joinToString(",")
        runCatching {
            repository.selectTorrentFiles(id, files.ifBlank { "all" })
            state.value = state.value.copy(message = "File selection sent.")
            refresh()
        }.onFailure {
            state.value = state.value.copy(message = it.message ?: "Could not select files.")
        }
    }

    fun generateLinks() = viewModelScope.launch {
        val links = state.value.torrent?.links.orEmpty()
        runCatching {
            state.value = state.value.copy(generating = true, message = null)
            val generated = repository.unrestrictLinks(links)
            state.value = state.value.copy(directLinks = generated, generating = false, message = null)
        }.onFailure {
            state.value = state.value.copy(generating = false, message = it.message ?: "Could not generate links.")
        }
    }

    fun delete(onDeleted: () -> Unit) = viewModelScope.launch {
        runCatching {
            repository.deleteTorrent(id)
            onDeleted()
        }.onFailure {
            state.value = state.value.copy(message = it.message ?: "Could not delete torrent.")
        }
    }
}

data class AccountState(val loading: Boolean = true, val user: RealDebridUser? = null, val message: String? = null)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val repository: DebbieRepository,
    private val auth: AuthRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    val state = MutableStateFlow(AccountState())

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        runCatching {
            state.value = state.value.copy(loading = true, message = null)
            state.value = AccountState(loading = false, user = repository.user())
        }.onFailure {
            state.value = state.value.copy(loading = false, message = it.message ?: "Could not load account.")
        }
    }

    fun setDarkTheme(enabled: Boolean) = viewModelScope.launch { settings.setDarkTheme(enabled) }
    fun setAutoUnrestrict(enabled: Boolean) = viewModelScope.launch { settings.setAutoUnrestrict(enabled) }
    fun disconnect() = auth.logout()
}
