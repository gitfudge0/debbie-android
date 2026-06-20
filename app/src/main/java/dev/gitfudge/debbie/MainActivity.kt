package dev.gitfudge.debbie

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.fragment.app.FragmentActivity
import androidx.mediarouter.media.MediaRouter
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DebbieApp() }
    }
}

@Composable
fun DebbieApp(sessionVm: SessionViewModel = hiltViewModel()) {
    val session by sessionVm.session.collectAsStateWithLifecycle()
    val settings by sessionVm.settings.collectAsStateWithLifecycle()
    DebbieTheme(darkTheme = settings.darkTheme) {
        DebbieSystemBars(darkTheme = settings.darkTheme)
        val nav = rememberNavController()
        val backStackEntry by nav.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        LaunchedEffect(session, currentRoute) {
            when {
                session != null && currentRoute in setOf("welcome", "login") -> nav.navigate("dashboard") { popUpTo(0) }
                session == null && currentRoute != null && currentRoute !in setOf("welcome", "login") -> nav.navigate("welcome") { popUpTo(0) }
            }
        }
        NavHost(navController = nav, startDestination = if (session == null) "welcome" else "dashboard") {
            composable("welcome") {
                WelcomeScreen(
                    darkTheme = settings.darkTheme,
                    onToggleTheme = { sessionVm.setDarkTheme(!settings.darkTheme) },
                    onSignIn = { nav.navigate("login") },
                )
            }
            composable("login") { LoginScreen() }
            composable("dashboard") {
                DashboardScreen(
                    openDetail = { nav.navigate("torrent/${it}") },
                    openAccount = { nav.navigate("account") },
                )
            }
            composable("account") { AccountScreen(settings = settings, back = { nav.popBackStack() }) }
            composable("torrent/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                DetailScreen(back = { nav.popBackStack() })
            }
        }
    }
}

@Composable
@Suppress("DEPRECATION")
fun DebbieSystemBars(darkTheme: Boolean) {
    val view = LocalView.current
    val color = MaterialTheme.colorScheme.background.toArgb()
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = color
        window.navigationBarColor = color
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

@Composable
fun WelcomeScreen(darkTheme: Boolean, onToggleTheme: () -> Unit, onSignIn: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val tightHeight = maxHeight < 660.dp
        val compactWidth = maxWidth < 380.dp
        val sidePadding = if (compactWidth) 20.dp else 24.dp
        val headlineSize = when {
            tightHeight -> 31.sp
            compactWidth -> 39.sp
            else -> 44.sp
        }
        val headlineLine = when {
            tightHeight -> 35.sp
            compactWidth -> 43.sp
            else -> 48.sp
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(start = sidePadding, end = sidePadding, bottom = if (tightHeight) 20.dp else 28.dp),
        ) {
            Spacer(Modifier.height(if (tightHeight) 18.dp else 72.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                LoginBrand()
                LoginThemeToggle(darkTheme = darkTheme, onToggle = onToggleTheme)
            }
            Spacer(Modifier.weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(if (tightHeight) 14.dp else 18.dp)) {
                Box(
                    Modifier
                        .size(if (tightHeight) 48.dp else 56.dp)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(0.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.AutoAwesome, "Debbie", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    "Real-Debrid,\nkept tidy.",
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = JetBrainsMono,
                        fontSize = headlineSize,
                        lineHeight = headlineLine,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    "Debbie helps you add torrents, watch active transfers, and open ready downloads without digging through the Real-Debrid site.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f),
                    fontFamily = JetBrainsMono,
                    fontSize = if (tightHeight) 13.sp else 15.sp,
                    lineHeight = if (tightHeight) 19.sp else 24.sp,
                )
            }
            Spacer(Modifier.height(if (tightHeight) 18.dp else 28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WelcomePoint("Add magnet links and torrent files")
                WelcomePoint("Track active, ready, and stuck transfers")
                WelcomePoint("Keep tokens private on this device")
            }
            Spacer(Modifier.height(if (tightHeight) 16.dp else 24.dp))
            LoginPrimaryButton(text = "SIGN IN", onClick = onSignIn, enabled = true)
        }
    }
}

@Composable
fun WelcomePoint(text: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(MaterialTheme.colorScheme.onSurface))
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f),
            fontFamily = JetBrainsMono,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

private fun screenPadding(scaffoldPadding: PaddingValues = PaddingValues(0.dp)) = PaddingValues(
    start = 20.dp,
    top = scaffoldPadding.calculateTopPadding() + 24.dp,
    end = 20.dp,
    bottom = scaffoldPadding.calculateBottomPadding() + 24.dp,
)

@Composable
fun ScreenHeader(title: String, subtitle: String? = null, navigation: (@Composable () -> Unit)? = null, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        navigation?.let {
            Box(contentAlignment = Alignment.Center) { it() }
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            subtitle?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f), fontSize = 13.sp)
            }
            Text(title, style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        action?.let {
            Spacer(Modifier.width(12.dp))
            Box(contentAlignment = Alignment.Center) { it() }
        }
    }
}

@Composable
fun LoginScreen(vm: LoginViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val darkTheme by vm.darkTheme.collectAsStateWithLifecycle()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val tightHeight = maxHeight < 660.dp
        val compactWidth = maxWidth < 380.dp
        val sidePadding = if (compactWidth) 20.dp else 24.dp
        val headerDrop = when {
            tightHeight -> 18.dp
            compactWidth -> 56.dp
            else -> 72.dp
        }
        val headlineSize = when {
            tightHeight -> 24.sp
            compactWidth -> 31.sp
            else -> 34.sp
        }
        val headlineLine = when {
            tightHeight -> 28.sp
            compactWidth -> 36.sp
            else -> 39.sp
        }
        val bodySize = if (tightHeight) 12.sp else 15.sp
        val bodyLine = if (tightHeight) 17.sp else 24.sp

        Column(
            Modifier
                .fillMaxSize()
                .padding(start = sidePadding, end = sidePadding, bottom = if (tightHeight) 20.dp else 28.dp),
        ) {
            Spacer(Modifier.height(headerDrop))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                LoginBrand()
                LoginThemeToggle(darkTheme = darkTheme, onToggle = { vm.setDarkTheme(!darkTheme) })
            }
            Spacer(Modifier.weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(if (tightHeight) 12.dp else 16.dp)) {
                Text(
                    "Sign in.\nWe stay out\nof your way.",
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = JetBrainsMono,
                        fontSize = headlineSize,
                        lineHeight = headlineLine,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    "Pick OAuth for a scoped device login, or paste a private API token. Debbie stores the bearer token in encrypted storage.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f),
                    fontFamily = JetBrainsMono,
                    fontSize = bodySize,
                    lineHeight = bodyLine,
                )
            }
            Spacer(Modifier.height(if (tightHeight) 12.dp else 24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LoginModeCard(
                    title = "OAuth",
                    subtitle = "Device code",
                    selected = state.mode == LoginMode.OAuth,
                    onClick = { vm.setMode(LoginMode.OAuth) },
                    modifier = Modifier.weight(1f),
                )
                LoginModeCard(
                    title = "API key",
                    subtitle = "Private token",
                    selected = state.mode == LoginMode.ApiKey,
                    onClick = { vm.setMode(LoginMode.ApiKey) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (state.mode == LoginMode.ApiKey) {
                Spacer(Modifier.height(12.dp))
                LoginApiKeyInput(state.apiKey, vm::setApiKey)
            }
            state.device?.let { device ->
                Spacer(Modifier.height(12.dp))
                LoginNotice {
                    KeyValue("Code", device.userCode)
                    Text(device.verificationUrl, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = JetBrainsMono, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(if (tightHeight) 10.dp else 16.dp))
            LoginPrimaryButton(
                text = when {
                    tightHeight && state.mode == LoginMode.OAuth -> "CONTINUE"
                    tightHeight -> "CONNECT"
                    state.mode == LoginMode.OAuth -> "CONTINUE WITH REAL-DEBRID"
                    else -> "CONNECT WITH API KEY"
                },
                onClick = { if (state.mode == LoginMode.OAuth) vm.startDeviceLogin() else vm.loginWithApiKey() },
                enabled = !state.busy && (state.mode == LoginMode.OAuth || state.apiKey.isNotBlank()),
            )
            if (!tightHeight) {
                Spacer(Modifier.height(20.dp))
                Text(
                    "PREMIUM SUBSCRIPTION REQUIRED",
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f),
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compactWidth) 11.sp else 12.sp,
                )
            }
            state.message?.let {
                Spacer(Modifier.height(12.dp))
                LoginNotice { Text(it, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f)) }
            }
        }
    }
}

@Composable
fun LoginBrand() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(15.dp).height(15.dp).background(MaterialTheme.colorScheme.onSurface))
        Text("debbie", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun LoginThemeToggle(darkTheme: Boolean, onToggle: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f), RoundedCornerShape(0.dp))
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (darkTheme) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
            if (darkTheme) "Switch to light theme" else "Switch to dark theme",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun LoginModeCard(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val border = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = .72f)
    Column(
        modifier
            .border(1.dp, border, RoundedCornerShape(0.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f), fontFamily = JetBrainsMono, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Composable
fun LoginApiKeyInput(value: String, onValueChange: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(0.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Private API token",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f),
            fontFamily = JetBrainsMono,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = JetBrainsMono,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
            ),
            decorationBox = { innerTextField ->
                Box(Modifier.fillMaxWidth()) {
                    if (value.isBlank()) {
                        Text(
                            "Paste token",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .4f),
                            fontFamily = JetBrainsMono,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
fun LoginPrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else .42f)
            .background(MaterialTheme.colorScheme.onSurface)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Outlined.Login, text, tint = MaterialTheme.colorScheme.background, modifier = Modifier.width(22.dp).height(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, color = MaterialTheme.colorScheme.background, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
fun LoginNotice(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f), RoundedCornerShape(0.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
fun DashboardScreen(openDetail: (String) -> Unit, openAccount: () -> Unit, vm: DashboardViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val torrents = state.torrents
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let(vm::addTorrentFile) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = screenPadding(WindowInsets.systemBars.asPaddingValues()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            ScreenHeader(
                title = "Welcome, ${state.user?.username ?: "back"}",
                subtitle = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.US)),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.showAdd(true) }) { Icon(Icons.Outlined.Add, "Add") }
                    IconButton(onClick = openAccount) { Icon(Icons.Outlined.AccountCircle, "Account") }
                }
            }
        }
        state.message?.let { item { HomeEmptyState("Notice", it) } }
        item { Text("Recent torrents", style = MaterialTheme.typography.titleMedium) }
        if (torrents.isEmpty()) {
            if (state.loading) items(4) { TorrentSkeletonRow(home = true) }
            else item { HomeEmptyState("No torrents", state.error ?: "Add a magnet or torrent file to begin.") }
        }
        items(torrents, key = { it.id }) { torrent ->
            HomeTorrentRow(torrent, onClick = { openDetail(torrent.id) })
        }
    }
    if (state.showAdd) {
        AlertDialog(
            onDismissRequest = { vm.showAdd(false) },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(0.dp),
            title = { Text("Add torrent") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DebbieInput(state.magnet, vm::setMagnet, "Magnet link", singleLine = false)
                    DebbieButton("Upload .torrent", onClick = { picker.launch("application/x-bittorrent") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton(onClick = vm::addMagnet, enabled = validateMagnet(state.magnet)) { Text("Add") } },
            dismissButton = { TextButton(onClick = { vm.showAdd(false) }) { Text("Cancel") } },
        )
    }
}

@Composable
fun HomeEmptyState(title: String, body: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f))
    }
}

@Composable
fun HomeTorrentRow(torrent: RealDebridTorrent, onClick: () -> Unit) {
    val tone = statusTone(torrent.status)
    val motionEnabled = rememberMotionEnabled()
    var visible by remember(torrent.id) { mutableStateOf(!motionEnabled) }
    LaunchedEffect(torrent.id, motionEnabled) {
        if (motionEnabled) visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 10 },
        exit = fadeOut(tween(120)) + slideOutVertically(tween(120)) { -it / 10 },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f), RoundedCornerShape(0.dp))
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Text(torrent.filename, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
                Pill(statusLabel(torrent.status), tone)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(fmtSize(torrent.bytes), fontFamily = JetBrainsMono, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                Text(shortHash(torrent.hash), modifier = Modifier.weight(1f), fontFamily = JetBrainsMono, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .4f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Outlined.ChevronRight, "Open", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .4f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun DetailScreen(back: () -> Unit, vm: DetailViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val torrent = state.torrent
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var castMedia by remember { mutableStateOf<CastableMedia?>(null) }
    var showCastOptions by remember { mutableStateOf(false) }
    var showCastRoutes by remember { mutableStateOf(false) }
    var castRoutesMedia by remember { mutableStateOf<CastableMedia?>(null) }
    // Media waiting for a Chromecast session to connect. The route sheet is dismissed
    // the instant a device is tapped, so the load must be driven from here (which stays
    // composed) — not from inside the sheet, whose listener would be torn down first.
    var pendingCastMedia by remember { mutableStateOf<CastableMedia?>(null) }
    DisposableEffect(context) {
        val sessionManager = runCatching {
            CastContext.getSharedInstance(context.applicationContext).sessionManager
        }.getOrNull() ?: return@DisposableEffect onDispose { }
        fun loadPending() {
            val media = pendingCastMedia ?: return
            pendingCastMedia = null
            vm.showMessage(castToChromecast(context, media))
        }
        val listener = object : SessionManagerListener<CastSession> {
            override fun onSessionStarted(session: CastSession, sessionId: String) = loadPending()
            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) = loadPending()
            override fun onSessionStarting(session: CastSession) {}
            override fun onSessionStartFailed(session: CastSession, error: Int) {}
            override fun onSessionEnding(session: CastSession) {}
            override fun onSessionEnded(session: CastSession, error: Int) {}
            override fun onSessionResuming(session: CastSession, sessionId: String) {}
            override fun onSessionResumeFailed(session: CastSession, error: Int) {}
            override fun onSessionSuspended(session: CastSession, reason: Int) {}
        }
        sessionManager.addSessionManagerListener(listener, CastSession::class.java)
        onDispose { sessionManager.removeSessionManagerListener(listener, CastSession::class.java) }
    }
    var dlnaDevices by remember { mutableStateOf<List<DlnaDevice>>(emptyList()) }
    var searchingDlna by remember { mutableStateOf(false) }
    val nearbyWifiPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.showMessage(if (granted) "Nearby device access granted. Search DLNA devices again." else "Nearby device access is needed for DLNA discovery.")
    }
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(Modifier.fillMaxSize()) {
        // Pinned header: back and refresh stay reachable however far the file list scrolls.
        Box(Modifier.padding(start = 20.dp, end = 20.dp, top = topInset + 24.dp, bottom = 12.dp)) {
            ScreenHeader(
                title = "Details",
                navigation = { IconButton(onClick = back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                action = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showCastOptions = true }) {
                            Icon(Icons.Outlined.Cast, "Cast")
                        }
                        IconButton(onClick = { vm.refresh() }) { Icon(Icons.Outlined.Refresh, "Refresh") }
                    }
                },
            )
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        torrent?.let {
            item { TorrentStatusBlock(it) }
            if (isNeedsAction(it.status)) {
                item {
                    DebbieCard {
                        Text("Select files", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DebbieButton("All", vm::selectAll, Modifier.weight(1f))
                            DebbieButton("None", vm::selectNone, Modifier.weight(1f))
                        }
                        DebbieButton("Start torrent", vm::submitFileSelection, Modifier.fillMaxWidth(), state.selectedFiles.isNotEmpty())
                    }
                }
                items(it.files, key = { file -> file.id }) { file ->
                    DebbieCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            DebbieCheckbox(checked = file.id in state.selectedFiles, onCheckedChange = { vm.toggleFile(file.id) }, accent = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text(file.path, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(fmtSize(file.bytes), fontFamily = JetBrainsMono, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                            }
                        }
                    }
                }
                item {
                    DebbieOutlinedButton("Delete torrent", { vm.delete(back) }, Modifier.fillMaxWidth(), destructive = true)
                }
            } else {
                // Working path first: detail, then the generate/grab actions and the
                // links they produce. The file list is reference, so it sits last.
                item { TorrentDetailsCard(it) }
                item {
                    DebbieCard {
                        DebbieButton(
                            if (state.generating) "Generating" else "Generate direct links",
                            vm::generateLinks,
                            Modifier.fillMaxWidth(),
                            enabled = it.links.isNotEmpty() && !state.generating,
                        )
                        DebbieOutlinedButton("Delete torrent", { vm.delete(back) }, Modifier.fillMaxWidth(), destructive = true)
                    }
                }
                if (state.directLinks.isNotEmpty()) {
                    item { SectionHeader("Direct links", "${state.directLinks.size}") }
                    items(state.directLinks, key = { link -> link.download }) { link ->
                        DownloadRow(link) { media -> castMedia = media }
                    }
                    if (state.directLinks.size > 1) {
                        item {
                            DebbieOutlinedButton(
                                "Copy all links",
                                { copyText(context, state.directLinks.joinToString("\n") { link -> link.download }) },
                                Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                if (it.files.isNotEmpty()) {
                    item { SectionHeader("Files", "${it.files.count { f -> f.selected == 1 }}/${it.files.size}") }
                    items(it.files, key = { file -> file.id }) { file -> TorrentFileRow(file) }
                }
            }
        }
        state.message?.let { item { EmptyState("Status", it) } }
        }
    }
    if (showCastRoutes) {
        CastRoutesSheet(
            media = castRoutesMedia,
            onStatus = { vm.showMessage(it) },
            onCastWhenConnected = { pendingCastMedia = it },
            onDismiss = {
                showCastRoutes = false
                castRoutesMedia = null
            },
        )
    }
    if (showCastOptions || castMedia != null) {
        val media = castMedia
        var useStream by remember { mutableStateOf(false) }
        var streamMedia by remember { mutableStateOf<CastableMedia?>(null) }
        var resolvingStream by remember { mutableStateOf(false) }
        // The media the cast targets actually use: original, or the transcoded copy if chosen.
        val effectiveMedia = if (useStream) streamMedia else media
        // Stream chosen but its URL isn't ready yet — block casting until it resolves.
        val streamPending = useStream && streamMedia == null
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                showCastOptions = false
                castMedia = null
                dlnaDevices = emptyList()
                searchingDlna = false
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dragHandle = null,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(44.dp)
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = .7f)),
                )
                Text(if (media == null) "Cast options" else "Cast media", style = MaterialTheme.typography.titleMedium)
                if (media != null) {
                    Text(
                        media.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f),
                    )
                }
                // Source picker: cast the original file, or a lower-bitrate Real-Debrid
                // transcode that buffers far less (handy for big 4K files).
                if (media?.sourceId != null && media.kind == CastMediaKind.Video) {
                    val selectOriginal = { useStream = false }
                    val selectStream = {
                        useStream = true
                        if (streamMedia == null && !resolvingStream) {
                            resolvingStream = true
                            vm.resolveStream(media) { resolved ->
                                streamMedia = resolved
                                resolvingStream = false
                                if (resolved == null) useStream = false
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (useStream) {
                            DebbieOutlinedButton("Original", selectOriginal, Modifier.weight(1f))
                            DebbieButton("Stream", selectStream, Modifier.weight(1f))
                        } else {
                            DebbieButton("Original", selectOriginal, Modifier.weight(1f))
                            DebbieOutlinedButton("Stream", selectStream, Modifier.weight(1f))
                        }
                    }
                    Text(
                        when {
                            resolvingStream -> "Preparing a lower-bitrate stream…"
                            useStream -> "Transcoded copy — less buffering, lower quality."
                            else -> "Original file — best quality, may buffer on 4K."
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f),
                    )
                }
                DebbieButton(
                    if (streamPending) "Preparing stream…" else "Chromecast",
                    {
                        showCastOptions = false
                        castRoutesMedia = effectiveMedia
                        castMedia = null
                        showCastRoutes = true
                    },
                    Modifier.fillMaxWidth(),
                    enabled = !streamPending,
                )
                DebbieOutlinedButton(
                    if (searchingDlna) "Searching DLNA" else "Find DLNA devices",
                    {
                        val selectedMedia = media
                        if (selectedMedia == null) {
                            vm.showMessage("Choose a media file before using DLNA.")
                            return@DebbieOutlinedButton
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED
                        ) {
                            nearbyWifiPermission.launch(Manifest.permission.NEARBY_WIFI_DEVICES)
                            return@DebbieOutlinedButton
                        }
                        searchingDlna = true
                        dlnaDevices = emptyList()
                        scope.launch {
                            runCatching { searchDlnaDevices(context) }
                                .onSuccess {
                                    dlnaDevices = it
                                    if (it.isEmpty()) vm.showMessage("No DLNA devices found.")
                                }
                                .onFailure { vm.showMessage(it.message ?: "Could not search DLNA devices.") }
                            searchingDlna = false
                        }
                    },
                    Modifier.fillMaxWidth(),
                    enabled = !searchingDlna,
                )
                dlnaDevices.forEach { device ->
                    DebbieOutlinedButton(
                        dlnaDeviceLabel(device),
                        {
                            val selectedMedia = effectiveMedia ?: return@DebbieOutlinedButton
                            scope.launch {
                                runCatching { castToDlnaDevice(device, selectedMedia) }
                                    .onSuccess { vm.showMessage(it) }
                                    .onFailure { vm.showMessage(it.message ?: "Could not start DLNA playback.") }
                                showCastOptions = false
                                castMedia = null
                                dlnaDevices = emptyList()
                            }
                        },
                        Modifier.fillMaxWidth(),
                    )
                }
                DebbieOutlinedButton(
                    "Close",
                    {
                        showCastOptions = false
                        castMedia = null
                        dlnaDevices = emptyList()
                        searchingDlna = false
                    },
                    Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// Compact status: a bare pill once settled (Ready/Error/Queued); pill + percent +
// progress bar + a speed · eta · seeders meta line only while actively transferring.
// Replaces the tall status card so a finished torrent reads as one line, not a block.
@Composable
fun TorrentStatusBlock(t: RealDebridTorrentInfo) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Pill(statusLabel(t.status), statusTone(t.status))
            if (isActive(t.status)) {
                Text(
                    "${t.progress.toInt()}%",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f),
                )
            }
        }
        if (isActive(t.status)) {
            ProgressBar(t.progress / 100.0)
            val meta = buildList {
                t.speed?.takeIf { it > 0 }?.let { speed ->
                    add(fmtSpeed(speed))
                    add(fmtEta(t.bytes, t.progress / 100.0, speed))
                }
                t.seeders?.let { add("$it seeders") }
            }
            if (meta.isNotEmpty()) {
                Text(
                    meta.joinToString("  ·  "),
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f),
                )
            }
        }
    }
}

@Composable
fun TorrentDetailsCard(t: RealDebridTorrentInfo) {
    DebbieCard {
        Text(
            t.filename,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 18.sp,
        )
        KeyValue("Size", fmtSize(t.bytes))
        KeyValue("Added", fmtDate(t.added))
        t.ended?.let { KeyValue("Completed", fmtDate(it)) }
        t.seeders?.let { KeyValue("Seeders", it.toString()) }
        KeyValue("Hash", shortHash(t.hash))
    }
}

// Section divider for the detail screen's list groups (Files, Direct links): a Title
// heading with a muted mono count pushed right. One header shape for both groups.
@Composable
fun SectionHeader(title: String, trailing: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            trailing,
            fontFamily = JetBrainsMono,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f),
        )
    }
}

// Read-only file row for a fetched torrent: name + size, unselected files dimmed to Faint.
@Composable
fun TorrentFileRow(file: RealDebridTorrentFile) {
    val selected = file.selected == 1
    val nameAlpha = if (selected) 1f else .4f
    val sizeAlpha = if (selected) .72f else .4f
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f), RoundedCornerShape(0.dp))
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            file.path.substringAfterLast('/'),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = nameAlpha),
        )
        Text(
            fmtSize(file.bytes),
            fontFamily = JetBrainsMono,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = sizeAlpha),
        )
    }
}

// Compact download row: filename over a size · host meta line, with copy/open
// actions pinned right. Replaces the tall Size/Host KeyValue card so the list
// reads as a dense instrument readout instead of three cards per screen.
@Composable
fun DownloadRow(download: RealDebridDownload, onCast: (CastableMedia) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f), RoundedCornerShape(0.dp))
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                download.filename,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(fmtSize(download.filesize), fontFamily = JetBrainsMono, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f))
                Text("·", fontFamily = JetBrainsMono, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .4f))
                Text(download.host, fontFamily = JetBrainsMono, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        DirectLinkActions(download.download, download.asCastableMedia(), onCast)
    }
}

@Composable
fun AccountScreen(settings: AppSettings, back: () -> Unit, vm: AccountViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val user = state.user
    LazyColumn(Modifier.fillMaxSize(), contentPadding = screenPadding(WindowInsets.systemBars.asPaddingValues()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ScreenHeader(
                "Account",
                navigation = { IconButton(onClick = back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
            )
        }
        item {
            DebbieCard {
                Text(user?.username ?: "Real-Debrid", fontWeight = FontWeight.Bold)
                KeyValue("Member id", user?.id?.toString() ?: "-")
                KeyValue("Type", user?.type ?: "-")
                KeyValue("Points", user?.points?.toString() ?: "0")
                KeyValue("Premium", "${premiumDaysLeft(user?.expiration)} days")
                KeyValue("Traffic", "0 GB of 2000 GB")
            }
        }
        item {
            DebbieCard {
                ToggleRow("Dark theme", settings.darkTheme, vm::setDarkTheme)
                ToggleRow("Auto-unrestrict", settings.autoUnrestrict, vm::setAutoUnrestrict)
                OpenButton("Real-Debrid", "https://real-debrid.com/")
                DebbieButton("Disconnect", vm::disconnect, Modifier.fillMaxWidth())
            }
        }
        state.message?.let { item { EmptyState("Status", it) } }
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        DebbieToggle(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun DirectLinkActions(link: String, castableMedia: CastableMedia?, onCast: (CastableMedia) -> Unit) {
    val context = LocalContext.current
    val iconTint = MaterialTheme.colorScheme.onSurface
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        castableMedia?.let { media ->
            IconButton(onClick = { openInPlayer(context, media.url, media.contentType) }) { Icon(Icons.Outlined.PlayArrow, "Play", tint = iconTint) }
            IconButton(onClick = { onCast(media) }) { Icon(Icons.Outlined.Cast, "Cast", tint = iconTint) }
        }
        IconButton(onClick = { copyText(context, link) }) { Icon(Icons.Outlined.ContentCopy, "Copy", tint = iconTint) }
        IconButton(onClick = { openUrl(context, link) }) { Icon(Icons.Outlined.OpenInBrowser, "Open", tint = iconTint) }
    }
}

@Composable
fun CastRoutesSheet(
    media: CastableMedia?,
    onStatus: (String) -> Unit,
    onCastWhenConnected: (CastableMedia) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val router = remember(context) { MediaRouter.getInstance(context.applicationContext) }
    val selector = remember(context) {
        runCatching { CastContext.getSharedInstance(context.applicationContext).mergedSelector }.getOrNull()
    }
    var routes by remember { mutableStateOf(castRoutes(router, selector)) }
    val selectedRoute = router.selectedRoute
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    DisposableEffect(router, selector) {
        if (selector == null) return@DisposableEffect onDispose { }
        val callback = object : MediaRouter.Callback() {
            private fun refresh() {
                routes = castRoutes(router, selector)
            }

            override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
            override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
            override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
            override fun onRouteSelected(router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) = refresh()
            override fun onRouteUnselected(router: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) = refresh()
        }
        router.addCallback(
            selector,
            callback,
            MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY or MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN,
        )
        routes = castRoutes(router, selector)
        onDispose { router.removeCallback(callback) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(44.dp)
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = .7f)),
            )
            Text("Cast devices", style = MaterialTheme.typography.titleMedium)
            if (selector == null) {
                Text("Chromecast is not available right now.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f))
            } else if (routes.isEmpty()) {
                Text("Looking for devices.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f))
            } else {
                routes.forEach { route ->
                    CastRouteRow(
                        route = route,
                        selected = route.id == selectedRoute.id,
                        onClick = {
                            route.select()
                            if (media != null) {
                                val current = runCatching {
                                    CastContext.getSharedInstance(context.applicationContext)
                                        .sessionManager.currentCastSession
                                }.getOrNull()
                                // Already connected: load now. Otherwise hand the media to the
                                // parent's session listener, which fires once connection completes.
                                if (current != null && current.isConnected) {
                                    onStatus(castToChromecast(context, media))
                                } else {
                                    onCastWhenConnected(media)
                                }
                            }
                            onDismiss()
                        },
                    )
                }
            }
            if (!selectedRoute.isDefault && !selectedRoute.isBluetooth) {
                DebbieOutlinedButton(
                    "Disconnect",
                    {
                        router.unselect(MediaRouter.UNSELECT_REASON_DISCONNECTED)
                        onDismiss()
                    },
                    Modifier.fillMaxWidth(),
                    destructive = true,
                )
            }
            DebbieOutlinedButton("Close", onDismiss, Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun CastRouteRow(route: MediaRouter.RouteInfo, selected: Boolean, onClick: () -> Unit) {
    val subtitle = when {
        selected -> "Connected"
        route.isConnecting -> "Connecting"
        route.description != null -> route.description.orEmpty()
        else -> castDeviceTypeLabel(route.deviceType)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f), RoundedCornerShape(0.dp))
            .clickable(enabled = route.isEnabled, onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Cast, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (route.isEnabled) 1f else .38f))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(route.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
        }
        if (selected) Pill("Active", selected = true)
    }
}

fun castRoutes(router: MediaRouter, selector: androidx.mediarouter.media.MediaRouteSelector?): List<MediaRouter.RouteInfo> {
    if (selector == null) return emptyList()
    return router.routes.filter { route ->
        route.isEnabled &&
            !route.isDefault &&
            !route.isBluetooth &&
            route.matchesSelector(selector)
    }
}

fun castDeviceTypeLabel(deviceType: Int): String = when (deviceType) {
    MediaRouter.RouteInfo.DEVICE_TYPE_TV -> "TV"
    MediaRouter.RouteInfo.DEVICE_TYPE_SPEAKER,
    MediaRouter.RouteInfo.DEVICE_TYPE_REMOTE_SPEAKER,
    -> "Speaker"
    MediaRouter.RouteInfo.DEVICE_TYPE_AUDIO_VIDEO_RECEIVER -> "Receiver"
    MediaRouter.RouteInfo.DEVICE_TYPE_COMPUTER -> "Computer"
    MediaRouter.RouteInfo.DEVICE_TYPE_GAME_CONSOLE -> "Game console"
    MediaRouter.RouteInfo.DEVICE_TYPE_GROUP -> "Group"
    else -> "Chromecast device"
}

@Composable
fun OpenButton(label: String, url: String) {
    val context = LocalContext.current
    DebbieButton(label, { openUrl(context, url) }, Modifier.fillMaxWidth())
}

fun copyText(context: Context, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Debbie link", value))
}

fun openUrl(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

fun openInPlayer(context: Context, url: String, contentType: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(url), contentType)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }.onFailure { openUrl(context, url) }
}

@Composable
fun SkeletonBlock(modifier: Modifier = Modifier, pulse: Float) {
    Box(
        modifier
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = .08f + .08f * pulse)),
    )
}

@Composable
private fun rememberSkeletonPulse(): Float {
    if (!rememberMotionEnabled()) return 0f
    val transition = rememberInfiniteTransition(label = "skeleton")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse",
    )
    return pulse
}

@Composable
private fun rememberMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    }
}

@Composable
fun TorrentSkeletonRow(home: Boolean) {
    val pulse = rememberSkeletonPulse()
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f), RoundedCornerShape(0.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SkeletonBlock(Modifier.weight(1f).height(16.dp), pulse)
            SkeletonBlock(Modifier.width(56.dp).height(16.dp), pulse)
        }
        SkeletonBlock(Modifier.fillMaxWidth(.7f).height(12.dp), pulse)
        if (!home) SkeletonBlock(Modifier.fillMaxWidth().height(6.dp), pulse)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SkeletonBlock(Modifier.width(64.dp).height(12.dp), pulse)
            SkeletonBlock(Modifier.weight(1f).height(12.dp), pulse)
            SkeletonBlock(Modifier.width(20.dp).height(12.dp), pulse)
        }
    }
}
