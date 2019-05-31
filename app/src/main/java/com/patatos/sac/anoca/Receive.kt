package com.patatos.sac.anoca

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class Receive : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("anoca::receiver", "Received ${intent?.action}")

        if (intent?.action == Intent.ACTION_USER_PRESENT) {
            val activity = Intent(context, MainActivity::class.java)
            activity.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            context!!.startActivity(activity)
        }
    }

}
