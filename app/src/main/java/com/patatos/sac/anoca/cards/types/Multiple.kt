package com.patatos.sac.anoca.cards.types

import android.os.Build
import android.support.v7.app.AlertDialog
import android.text.Html
import android.text.SpannedString
import android.view.View
import android.widget.Button

import com.patatos.sac.anoca.R
import com.patatos.sac.anoca.cards.BaseCard

import kotlinx.android.synthetic.main.card_multiple.view.*
import kotlin.random.Random

class Multiple : BaseCard(4) {

    private lateinit var answer: CharSequence

    override fun makeBuilder(): AlertDialog.Builder {
        // Create builder
        val builder = super.baseBuilder(R.layout.card_multiple)

        val order = if (Random.nextBoolean()) Pair(0, 1) else Pair(1, 0)

        // Setup front
        super.adaptContent(this.layout.front, order.first).setOnClickListener {
            this.layout.front.visibility = View.GONE
            this.layout.back.visibility = View.VISIBLE
        }

        // Setup back

        // Randomize matches
        val association = mutableListOf(order.second, 2 + order.second, 4 + order.second, 6 + order.second).shuffled()
        val buttons = listOf(this.layout.tr, this.layout.tl, this.layout.br, this.layout.bl)

        // Setup matches
        buttons.forEachIndexed { k: Int, b: Button ->
            super.adaptContent(b, association[k]).let {
                it.setOnClickListener {
                    if (association[k] == order.second)
                        super.submitStatus(Companion.Status.ANSWERED_RIGHT)
                    else super.submitStatus(Companion.Status.ANSWERED_WRONG)
                }
                if (association[k] == order.second)
                    this.answer = it.text
            }
        }

        return builder
    }

    override fun finally(status: Companion.Status) {
        if (status == Companion.Status.ANSWERED_RIGHT)
            super.toast(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) Html.fromHtml("&check;", 0)
                else SpannedString("V")
            )
        else super.toast(this.answer)
    }

}
