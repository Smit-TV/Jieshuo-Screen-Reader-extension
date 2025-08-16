package com.android.talkback
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.os.Bundle
import android.net.Uri
import android.view.Gravity
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.Preference

class PreferenceFragment(val fragmentResource: Int) : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = "AppSettings"
        preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
        setPreferencesFromResource(fragmentResource, rootKey)

        val accessibilitySettings = findPreference<Preference>("accessibility_settings")
        accessibilitySettings?.setOnPreferenceClickListener { _ ->
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        true
        }

        val officialTgChannel = findPreference<Preference>("official_tg_channel")
        officialTgChannel?.setOnPreferenceClickListener { _ ->
        openLink("https://t.me/smit_tv_official")
        true
        }

        val donate = findPreference<Preference>("donate")
        donate?.setVisible(false)
        donate?.setOnPreferenceClickListener { _ ->
        val dialog = AlertDialog.Builder(requireContext(), R.style.app_theme)
        .setTitle(R.string.donate)
        .setMessage(R.string.donate_message)
        .setNegativeButton(R.string.cancel) { dialog, _ ->
        dialog.dismiss()
        }
        .setPositiveButton(R.string.continue_donate) { dialog, _ ->
        openLink("https://donationalerts.com/")
        }
        .create()

        dialog.getWindow()?.let {
            it.setGravity(Gravity.CENTER)
            it.setLayout(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
        true
        }

        val getLogs = findPreference<Preference>("get_logs")
        getLogs?.setOnPreferenceClickListener { _ ->
        LogUtils.createDebugFile(requireContext())
        true
        }
    }

    fun openLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        startActivity(intent)
    }
}