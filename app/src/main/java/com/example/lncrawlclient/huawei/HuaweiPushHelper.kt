package com.example.lncrawlclient.huawei

import android.content.Context
import android.util.Log

/**
 * Huawei Push Kit helper (stubbed)
 *
 * To enable Huawei Push Kit, you must:
 * 1. Add HMS SDK dependency in app/build.gradle:
 *    implementation 'com.huawei.hms:push:6.6.0.300' (check latest)
 * 2. Add agconnect-services.json to app/ (from AppGallery Connect)
 * 3. Register a service in AndroidManifest (see README instructions)
 * 4. Provide logic to send push/local notifications with obtained token.
 *
 * The methods below are placeholders — they show how to detect HMS availability and where to register token handling.
 */
object HuaweiPushHelper {
    private const val TAG = "HuaweiPushHelper"

    fun isHuaweiDevice(): Boolean {
        return android.os.Build.MANUFACTURER.equals("HUAWEI", ignoreCase = true) ||
               android.os.Build.MANUFACTURER.equals("HONOR", ignoreCase = true)
    }

    fun isHmsAvailable(context: Context): Boolean {
        // Simple heuristic: check if HMS AGC config file exists
        val res = context.resources
        val pkgName = context.packageName
        val id = res.getIdentifier("agconnect_services_json", "raw", pkgName)
        if (id != 0) return true
        // Additionally you can check for HMS Core package presence via package manager
        return try {
            context.packageManager.getPackageInfo("com.huawei.hwid", 0)
            true
        } catch (e: Exception) {
            Log.w(TAG, "HMS Core not present: ${e.message}")
            false
        }
    }

    fun registerPush(context: Context) {
        // Placeholder: in production, initialize HMS and request a push token.
        Log.i(TAG, "registerPush called - make sure you've added HMS SDK and agconnect-services.json")
    }

    fun fallbackNotify(context: Context, title: String, msg: String) {
        // In case system notifications are blocked, use Push Kit or local workaround.
        Log.i(TAG, "fallbackNotify: $title - $msg") 
    }
}
