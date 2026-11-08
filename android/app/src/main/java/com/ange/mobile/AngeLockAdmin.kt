package com.ange.mobile

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** Optional Android-admin role limited by XML to force-lock; never enrolls an owner. */
class AngeLockAdmin : DeviceAdminReceiver() {
    companion object {
        const val REQUEST_LOCK_PERMISSION="com.ange.mobile.REQUEST_LOCK_PERMISSION"
        fun component(context: Context)=ComponentName(context,AngeLockAdmin::class.java)
        fun manager(context: Context)=context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        fun active(context: Context)=manager(context).isAdminActive(component(context))
        fun request(context: Context)=Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,component(context))
            .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,"ANGE demande uniquement le droit de verrouiller l’écran sur votre commande. Aucun effacement, aucun changement de mot de passe. Vous pouvez retirer ce droit dans les réglages Android.")
    }
}
