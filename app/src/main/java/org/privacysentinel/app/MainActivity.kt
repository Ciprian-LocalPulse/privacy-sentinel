package org.privacysentinel.app

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SentinelTheme { SentinelApp() } }
    }
}

private fun safeStart(ctx: Context, intent: Intent) {
    try {
        ctx.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Some OEM builds lack certain settings screens; ignore.
    }
}

@Composable
fun SentinelApp() {
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var monitoring by remember { mutableStateOf(Prefs.monitoringEnabled(ctx)) }
    var resumeTick by remember { mutableIntStateOf(0) }

    // Re-check permissions / app list whenever the user comes back to the app.
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeTick++
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val tabTitles = listOf(R.string.tab_log, R.string.tab_apps, R.string.tab_settings)

    Scaffold { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.monitoring_title)) },
                supportingContent = { Text(stringResource(R.string.monitoring_desc)) },
                trailingContent = {
                    Switch(
                        checked = monitoring,
                        onCheckedChange = { on ->
                            monitoring = on
                            Prefs.setMonitoringEnabled(ctx, on)
                            val intent = Intent(ctx, MonitorService::class.java)
                            if (on) ContextCompat.startForegroundService(ctx, intent)
                            else ctx.stopService(intent)
                        }
                    )
                }
            )
            TabRow(selectedTabIndex = tab) {
                tabTitles.forEachIndexed { index, titleRes ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = { Text(stringResource(titleRes)) }
                    )
                }
            }
            when (tab) {
                0 -> LogTab()
                1 -> AppsTab(resumeTick)
                else -> SettingsTab(resumeTick)
            }
        }
    }
}

@Composable
fun LogTab() {
    val ctx = LocalContext.current
    val store = remember { EventStore.get(ctx) }
    val version by EventStore.changes.collectAsState()
    var events by remember { mutableStateOf(emptyList<SensorEvent>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(version) {
        events = withContext(Dispatchers.IO) { store.recent() }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { scope.launch(Dispatchers.IO) { store.clear() } },
                enabled = events.isNotEmpty()
            ) { Text(stringResource(R.string.clear_log)) }
        }
        if (events.isEmpty()) {
            Text(
                stringResource(R.string.empty_log),
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn {
                items(events, key = { it.id }) { e ->
                    EventRow(e)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun EventRow(e: SensorEvent) {
    val ctx = LocalContext.current
    val titleRes = when {
        e.sensor == SensorKind.CAMERA && e.state == EventState.START -> R.string.event_camera_start
        e.sensor == SensorKind.CAMERA -> R.string.event_camera_stop
        e.state == EventState.START -> R.string.event_mic_start
        else -> R.string.event_mic_stop
    }
    val title = stringResource(titleRes)
    val screenOffText = stringResource(R.string.screen_off)
    val unknownText = stringResource(R.string.unknown_app)
    val appLabel = remember(e.app) { e.app?.let { ForegroundAppResolver.label(ctx, it) } }
    val probableText = if (appLabel != null) stringResource(R.string.probable_app, appLabel) else null

    val time = remember(e.timestamp) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
            .format(Date(e.timestamp))
    }
    val parts = mutableListOf(time)
    if (!e.screenOn) parts.add(screenOffText)
    if (e.state == EventState.START) parts.add(probableText ?: unknownText)

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(parts.joinToString(" · ")) },
        trailingContent = {
            if (e.suspicious) {
                Text(
                    stringResource(R.string.suspicious),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}

@Composable
fun AppsTab(resumeTick: Int) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<AppAccess>?>(null) }

    LaunchedEffect(resumeTick) {
        apps = withContext(Dispatchers.IO) { PermissionAuditor.scan(ctx) }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.apps_intro),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedButton(
            onClick = { safeStart(ctx, Intent(Settings.ACTION_PRIVACY_SETTINGS)) },
            modifier = Modifier.padding(horizontal = 16.dp)
        ) { Text(stringResource(R.string.privacy_settings)) }

        val list = apps
        when {
            list == null -> CircularProgressIndicator(Modifier.padding(24.dp))
            list.isEmpty() -> Text(
                stringResource(R.string.apps_empty),
                modifier = Modifier.padding(16.dp)
            )
            else -> LazyColumn {
                items(list, key = { it.packageName }) { app ->
                    AppRow(app)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun AppRow(app: AppAccess) {
    val ctx = LocalContext.current
    val allowed = stringResource(R.string.perm_allowed)
    val denied = stringResource(R.string.perm_denied)
    val none = stringResource(R.string.perm_none)
    fun text(v: Boolean?) = when (v) {
        true -> allowed
        false -> denied
        null -> none
    }
    val camera = stringResource(R.string.perm_camera)
    val mic = stringResource(R.string.perm_mic)

    ListItem(
        headlineContent = { Text(app.label) },
        supportingContent = {
            Text("$camera: ${text(app.camera)} · $mic: ${text(app.mic)}")
        },
        trailingContent = {
            TextButton(onClick = {
                safeStart(
                    ctx,
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", app.packageName, null)
                    )
                )
            }) { Text(stringResource(R.string.open_settings)) }
        }
    )
}

@Composable
fun SettingsTab(resumeTick: Int) {
    val ctx = LocalContext.current
    val usageGranted = remember(resumeTick) { ForegroundAppResolver.hasUsageAccess(ctx) }
    var alertAll by remember { mutableStateOf(Prefs.alertAll(ctx)) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 8.dp)
    ) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.usage_access_title)) },
            supportingContent = { Text(stringResource(R.string.usage_access_desc)) },
            trailingContent = {
                if (usageGranted) {
                    Text(stringResource(R.string.granted))
                } else {
                    Button(onClick = {
                        safeStart(ctx, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }) { Text(stringResource(R.string.grant)) }
                }
            }
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(stringResource(R.string.alert_all_title)) },
            supportingContent = { Text(stringResource(R.string.alert_all_desc)) },
            trailingContent = {
                Switch(checked = alertAll, onCheckedChange = {
                    alertAll = it
                    Prefs.setAlertAll(ctx, it)
                })
            }
        )
        HorizontalDivider()
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.limits_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                stringResource(R.string.limits_text),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
