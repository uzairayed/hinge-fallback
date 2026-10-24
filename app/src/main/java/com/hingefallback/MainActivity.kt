package com.hingefallback

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.hingefallback.databinding.ActivityMainBinding
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var suppressSwitch = false

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) enableWatcher() else toast(getString(R.string.notification_required))
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, result ->
        if (result == PackageManager.PERMISSION_GRANTED) enableWatcher()
        render()
    }

    private val statusListener = { runOnUiThread { render() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        StatusStore.listen(statusListener)

        binding.allowButton.setOnClickListener { requestShizuku() }
        binding.enableSwitch.setOnCheckedChangeListener { _, checked ->
            if (suppressSwitch) return@setOnCheckedChangeListener
            if (checked) enableWatcher() else disableWatcher()
        }
        render()
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        StatusStore.unlisten(statusListener)
        super.onDestroy()
    }

    private fun enableWatcher() {
        if (!ShizukuShell.isManagerInstalled(packageManager)) {
            setSwitch(false)
            toast(getString(R.string.shizuku_missing))
            return
        }
        if (!ShizukuShell.isRunning()) {
            setSwitch(false)
            toast(getString(R.string.shizuku_stopped))
            return
        }
        if (!ShizukuShell.isGranted()) {
            setSwitch(false)
            requestShizuku()
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            setSwitch(false)
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        Prefs.setEnabled(this, true)
        ContextCompat.startForegroundService(this, Intent(this, HingeFallbackService::class.java))
        requestBatteryExemption()
        render()
    }

    private fun disableWatcher() {
        Prefs.setEnabled(this, false)
        startService(Intent(this, HingeFallbackService::class.java).setAction(HingeFallbackService.ACTION_DISABLE))
        render()
    }

    private fun requestShizuku() {
        if (!ShizukuShell.isRunning()) {
            toast(getString(R.string.shizuku_stopped))
            return
        }
        Shizuku.requestPermission(REQUEST_SHIZUKU)
    }

    private fun requestBatteryExemption() {
        val power = getSystemService(android.os.PowerManager::class.java)
        if (power.isIgnoringBatteryOptimizations(packageName)) return
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:$packageName"),
        )
        runCatching { startActivity(intent) }
    }

    private fun render() {
        val managerInstalled = ShizukuShell.isManagerInstalled(packageManager)
        val running = ShizukuShell.isRunning()
        val granted = ShizukuShell.isGranted()
        binding.shizukuValue.text = when {
            !managerInstalled -> getString(R.string.shizuku_missing)
            !running -> getString(R.string.shizuku_stopped)
            else -> getString(R.string.shizuku_running)
        }
        binding.permissionValue.text = if (granted) getString(R.string.allowed) else getString(R.string.needed)
        binding.hall0Value.text = StatusStore.hall0
        binding.hall1Value.text = StatusStore.hall1
        binding.stateValue.text = StatusStore.deviceState
        binding.errorText.text = StatusStore.lastError
        binding.allowButton.isEnabled = running && !granted
        setSwitch(Prefs.isEnabled(this))
    }

    private fun setSwitch(checked: Boolean) {
        suppressSwitch = true
        binding.enableSwitch.isChecked = checked
        suppressSwitch = false
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val REQUEST_SHIZUKU = 1
    }
}
