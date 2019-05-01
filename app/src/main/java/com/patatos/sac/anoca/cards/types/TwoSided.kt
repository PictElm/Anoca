package com.patatos.sac.anoca.cards.types

import android.support.v7.app.AlertDialog
import android.view.View

import com.patatos.sac.anoca.R
import com.patatos.sac.anoca.cards.BaseCard
import com.patatos.sac.anoca.cards.Status

import kotlinx.android.synthetic.main.card_twosided.view.*
import kotlin.random.Random

class TwoSided : BaseCard(1) {

    override fun makeBuilder(): AlertDialog.Builder {
        // Create builder
        val builder = super.baseBuilder(R.layout.card_twosided)

        val order = if (Random.nextBoolean()) Pair(0, 1) else Pair(1, 0)

        // Setup front
        super.adaptContent(this.layout.front, order.first).setOnClickListener {
            this.layout.front.visibility = View.GONE
            this.layout.back.visibility = View.VISIBLE
        }

        // Setup back
        super.adaptContent(this.layout.back, order.second).setOnClickListener {
            this.layout.front.visibility = View.VISIBLE
            this.layout.back.visibility = View.GONE
        }

        super.setStatus(Status.ANSWERED)

        return builder
    }

}
