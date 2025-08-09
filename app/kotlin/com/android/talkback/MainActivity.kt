package com.android.talkback
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager
        .beginTransaction()
        .replace(android.R.id.content, PreferenceFragment(R.xml.settings_preferences))
        .commit()
    }
}