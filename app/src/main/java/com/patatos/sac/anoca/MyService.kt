package com.patatos.sac.anoca

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log

class MyService : Service() {

    private val receiver = Receive()
    private var registered = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("anoca::service", "started")

        if (!this.registered) {
            this.registerReceiver(this.receiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
            this.registered = true
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.i("anoca::service", "destroyed")

        if (this.registered) {
            this.unregisterReceiver(this.receiver)
            this.registered = false
        }

        if (this.getSharedPreferences(this.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
                .getBoolean(getString(R.string.master_switch_key), true))
            this.sendBroadcast(Intent(this, Receive::class.java).setAction(ACTION_SERVICE_DESTROYED))
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

}
