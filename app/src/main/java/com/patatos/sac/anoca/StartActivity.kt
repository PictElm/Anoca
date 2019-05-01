package com.patatos.sac.anoca

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.startService(Intent(this, MyService::class.java))
        this.finish()
    }

}
