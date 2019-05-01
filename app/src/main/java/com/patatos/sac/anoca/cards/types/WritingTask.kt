package com.patatos.sac.anoca.cards.types

import android.os.Build
import android.support.v7.app.AlertDialog
import android.text.Html
import android.text.Spanned
import android.text.SpannedString
import android.view.inputmethod.EditorInfo

import com.patatos.sac.anoca.R
import com.patatos.sac.anoca.cards.BaseCard
import com.patatos.sac.anoca.cards.Status

import kotlinx.android.synthetic.main.card_writingtask.view.*
import kotlin.random.Random

class WritingTask : BaseCard(1) {

    private lateinit var answerMatch: String
    private lateinit var answerToast: Spanned

    override fun makeBuilder(): AlertDialog.Builder {
        // Create builder
        val builder = super.baseBuilder(R.layout.card_writingtask, "Ok")

        // Setup question
        val question = if (Random.nextBoolean()) Pair(0, 1) else Pair(1, 0)
        super.adaptContent(this.layout.question, question.first)
        this.answerMatch = this.content.getText(question.second)
        this.answerToast = this.content.getSpanned(question.second)

        // Setup answer
        this.layout.answer.setOnEditorActionListener { _, id, event ->
            if (id == EditorInfo.IME_NULL || event != null) { // user pressed 'Enter' TODO: doesn't work...
                this.positive()
                true
            } else false
        }

        return builder
    }

    override fun positive() {
        super.submitStatus(
            if (super.stringMatch(this.layout.answer.text.toString(), this.answerMatch))
                Status.ANSWERED_RIGHT
            else Status.ANSWERED_WRONG
        )
    }

    override fun negative() {
        super.submitStatus(Status.ANSWERED)
    }


    override fun finally(status: Status) {
        if (status == Status.ANSWERED_RIGHT)
            super.toast(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) Html.fromHtml("&check;", 0)
                else SpannedString("V")
            )
        else super.toast(this.answerToast)
    }

}
