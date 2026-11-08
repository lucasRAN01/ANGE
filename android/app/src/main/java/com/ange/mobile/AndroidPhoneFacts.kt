package com.ange.mobile

import android.content.Context
import android.os.BatteryManager
import java.util.Date
import java.util.TimeZone

internal object AndroidPhoneFacts {
    fun read(context: Context): PhoneSnapshot {
        val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = runCatching { manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) }
            .getOrNull()?.takeIf { it in 0..100 }
        val charging = runCatching { manager?.isCharging }.getOrNull()
        return PhoneSnapshot(Date(), TimeZone.getDefault(), level, charging)
    }
}
