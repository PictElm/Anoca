package com.patatos.sac.anoca

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class Receive : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_SCREEN_OFF) {
            val activity = Intent(context, MainActivity::class.java)
            activity.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            context!!.startActivity(activity)
        }
    }

}
