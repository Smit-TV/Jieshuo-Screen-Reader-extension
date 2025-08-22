package com.android.talkback
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import androidx.appcompat.app.AppCompatActivity

class TalkBackPreferencesActivity : AppCompatActivity() {
    private lateinit var powerManager: PowerManager

	override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
        supportFragmentManager
        .beginTransaction()
        .replace(android.R.id.content, PreferenceFragment(R.xml.settings_preferences))
        .commit()
    }
}