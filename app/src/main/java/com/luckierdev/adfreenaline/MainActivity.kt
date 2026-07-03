package com.luckierdev.adfreenaline

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luckierdev.adfreenaline.ui.AppRoot
import com.luckierdev.adfreenaline.ui.theme.AdFreenalineTheme
import org.maplibre.android.MapLibre
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PREFS_PERMISSIONS = "permissions"
private const val KEY_LOCATION_REQUESTED = "location_requested"

class MainActivity : ComponentActivity() {
    private val viewModel: RunViewModel by viewModels()
    private var hasLocationPermission by mutableStateOf(false)
    private var locationPermissionBlocked by mutableStateOf(false)
    private var hasNotificationPermission by mutableStateOf(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
    private var snackbarMessage by mutableStateOf<String?>(null)
    private val permissionPrefs by lazy { getSharedPreferences(PREFS_PERMISSIONS, MODE_PRIVATE) }

    private fun showMessage(message: String) {
        snackbarMessage = message
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        hasLocationPermission = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (hasLocationPermission) {
            locationPermissionBlocked = false
            viewModel.startLocationWatch()
        } else {
            locationPermissionBlocked = isLocationPermissionBlocked()
            val message = if (locationPermissionBlocked) {
                getString(R.string.msg_location_denied_settings)
            } else {
                getString(R.string.msg_location_required)
            }
            showMessage(message)
        }
    }
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        if (!hasNotificationPermission) {
            showMessage(getString(R.string.msg_notifications_info))
        }
    }
    private val ringtonePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            viewModel.updateSettings { it.copy(goalAlertSoundUri = uri?.toString()) }
        }
    }
    private val exportCsvLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        val records = viewModel.history.value
        if (records.isEmpty()) {
            showMessage(getString(R.string.msg_no_runs_to_export))
            return@registerForActivityResult
        }
        runCatching {
            val csv = RunHistoryExporter.toCsv(records)
            contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(csv.toByteArray(Charsets.UTF_8))
            } ?: error("Could not open file")
            showMessage(getString(R.string.msg_exported_runs, records.size))
        }.onFailure {
            showMessage(getString(R.string.msg_export_failed, it.message ?: ""))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MapLibre.getInstance(this)
        requestNotificationPermissionIfNeeded()
        promptLocationPermissionIfNeeded()

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            AdFreenalineTheme(themeMode = settings.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(
                        vm = viewModel,
                        hasLocationPermission = hasLocationPermission,
                        locationPermissionBlocked = locationPermissionBlocked,
                        requestPermission = ::requestLocation,
                        onPickGoalSound = ::launchGoalSoundPicker,
                        onPreviewGoalSound = viewModel::previewGoalSound,
                        onExportRuns = ::launchExportCsv,
                        snackbarMessage = snackbarMessage,
                        onSnackbarShown = { snackbarMessage = null }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        syncLocationPermissionState()
    }

    private fun syncLocationPermissionState() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        hasLocationPermission = granted
        locationPermissionBlocked = !granted && isLocationPermissionBlocked()
        if (granted) viewModel.startLocationWatch()
    }

    private fun isLocationPermissionBlocked(): Boolean {
        return permissionPrefs.getBoolean(KEY_LOCATION_REQUESTED, false) &&
            !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun promptLocationPermissionIfNeeded() {
        syncLocationPermissionState()
        if (hasLocationPermission || locationPermissionBlocked) return
        requestLocationPermissionDialog()
    }

    private fun requestLocation() {
        syncLocationPermissionState()
        if (hasLocationPermission) return
        if (locationPermissionBlocked) {
            openAppSettings()
            return
        }
        requestLocationPermissionDialog()
    }

    private fun requestLocationPermissionDialog() {
        permissionPrefs.edit().putBoolean(KEY_LOCATION_REQUESTED, true).apply()
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun openAppSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
        )
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        hasNotificationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun launchGoalSoundPicker() {
        val current = viewModel.settings.value.goalAlertSoundUri
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            if (!current.isNullOrBlank()) {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(current))
            }
        }
        ringtonePickerLauncher.launch(intent)
    }

    private fun launchExportCsv() {
        if (viewModel.history.value.isEmpty()) {
            showMessage(getString(R.string.msg_no_runs_to_export))
            return
        }
        val filename = "adfreenaline_runs_${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.csv"
        exportCsvLauncher.launch(filename)
    }
}
