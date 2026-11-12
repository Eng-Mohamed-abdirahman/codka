package com.codka.app.sms

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Phone settings Codka needs so payment SMS reach it while the app is closed.
 *
 * Found on an Infinix X6886 (Android 16): the Transsion freezer ("Hiber") drops the
 * SMS_RECEIVED broadcast for a frozen app ("Hiber/broadcast restricted queue").
 * It only works after the user allows Codka in Phone Master > App Launch and sets
 * battery to "No restrictions". Infinix, Tecno and itel all ship this freezer.
 */
object BackgroundSetup {
    private val TRANSSION_BRANDS = setOf("infinix", "tecno", "itel")

    /** Phone Master's App Launch (auto-start) screen, tried in order. */
    val TRANSSION_AUTO_START_SCREENS =
        listOf(
            "com.transsion.phonemaster" to "com.cyin.himgr.autostart.AutoStartActivity"
        )

    const val TRANSSION_AUTO_START_ACTION =
        "com.cyin.himgr.applicationmanager.view.activities.AUTO_START_ACTIVITY"

    fun isTranssion(manufacturer: String = Build.MANUFACTURER): Boolean =
        manufacturer.trim().lowercase() in TRANSSION_BRANDS

    fun isBatteryUnrestricted(context: Context): Boolean {
        val power = context.getSystemService(PowerManager::class.java) ?: return false
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Shows Android's "let Codka run in the background?" dialog. */
    fun openBatterySetting(context: Context) {
        val packageUri = Uri.parse("package:${context.packageName}")
        tryStart(
            context,
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri),
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            appDetails(context)
        )
    }

    /** Opens Phone Master > App Launch, or Codka's app info page if it isn't there. */
    fun openAutoStartSetting(context: Context) {
        val screens =
            TRANSSION_AUTO_START_SCREENS.map { (pkg, cls) ->
                Intent().setComponent(ComponentName(pkg, cls))
            }
        tryStart(
            context,
            *screens.toTypedArray(),
            Intent(TRANSSION_AUTO_START_ACTION),
            appDetails(context)
        )
    }

    private fun appDetails(context: Context) = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:${context.packageName}")
    )

    private fun tryStart(context: Context, vararg intents: Intent) {
        for (intent in intents) {
            // From an Activity, open the screen on top of Codka so Back returns here.
            if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                // try the next screen
            } catch (_: SecurityException) {
                // not exported on this phone; try the next screen
            }
        }
    }
}
