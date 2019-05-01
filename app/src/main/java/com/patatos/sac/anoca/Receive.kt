package com.patatos.sac.anoca

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

const val ACTION_SERVICE_DESTROYED = "anoca.intent.action.SERVICE_DESTROYED"

class Receive : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == ACTION_SERVICE_DESTROYED) {
            val activity = Intent(context, StartActivity::class.java)
            activity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context!!.startActivity(activity)
        } else if(intent?.action == Intent.ACTION_SCREEN_OFF) {
            val activity = Intent(context, MainActivity::class.java)
            activity.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            context!!.startActivity(activity)
        }
    }

}
