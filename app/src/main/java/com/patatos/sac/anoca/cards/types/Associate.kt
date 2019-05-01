package com.patatos.sac.anoca.cards.types

import android.support.v7.app.AlertDialog
import android.widget.Button

import com.patatos.sac.anoca.R
import com.patatos.sac.anoca.cards.BaseCard
import com.patatos.sac.anoca.cards.Status

import kotlinx.android.synthetic.main.card_associate.view.*
import kotlin.random.Random

class Associate : BaseCard(2) {

    override fun makeBuilder(): AlertDialog.Builder {
        // Create builder
        val builder = super.baseBuilder(R.layout.card_associate)

        // Randomize matches
        val association = mutableListOf(0, 1, 2, 3).let { ids ->
            List(ids.count()) { ids.removeAt(Random.nextInt(ids.count())) }
        }
        val buttons = listOf(this.layout.tr, this.layout.tl, this.layout.br, this.layout.bl)

        // Setup matches
        var selected = -1
        var progress = 0

        buttons.forEachIndexed { k: Int, b: Button ->
            super.adaptContent(b, association[k]).setOnClickListener {
                if (selected < 0) {
                    buttons[k].isClickable = false
                    buttons[k].isEnabled = false
                    selected = k
                } else if (association[selected] == association[k] + if (association[k] % 2 == 0) 1 else -1) {
                    buttons[k].isClickable = false
                    buttons[k].isEnabled = false
                    selected = -1
                    if (++progress == 2) super.submitStatus(Status.ANSWERED_RIGHT)
                } else {
                    buttons[selected].isClickable = true
                    buttons[selected].isEnabled = true
                    selected = -1
                }
            }
        }

        return builder
    }

}
