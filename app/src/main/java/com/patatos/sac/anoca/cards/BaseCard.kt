package com.patatos.sac.anoca.cards

import android.app.Dialog
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v4.app.DialogFragment
import android.support.v4.widget.TextViewCompat
import android.support.v7.app.AlertDialog
import android.text.Layout
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.Toast

import com.patatos.sac.anoca.MainActivity

abstract class BaseCard(val contentSize: Int?) : DialogFragment() {

    private var builder: AlertDialog.Builder? = null
    private lateinit var activity: MainActivity
    protected lateinit var content: Content
    protected lateinit var layout: View

    private var status: Status = Status.UNINITIALIZED
    private var statusSubmitted = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        if (savedInstanceState != null)
            this.setContent(
                Content(
                    this.context as MainActivity,
                    savedInstanceState.getStringArray(MainActivity.SAVED_CONTENT_KEY)!!.toList(),
                    savedInstanceState.getString(MainActivity.SAVED_TITLE_KEY)!!
                )
            )

        this.status = Status.WAITING
        return this.builder!!.create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putStringArray(MainActivity.SAVED_CONTENT_KEY, this.content.saveRaw())
        outState.putString(MainActivity.SAVED_TITLE_KEY, this.content.categoryName)

        if (this.status != Status.DISMISSED)
            this.status = Status.SAVED
    }

    protected abstract fun makeBuilder(): AlertDialog.Builder

    fun setContent(content: Content): BaseCard {
        this.content = content
        this.activity = content.getActivity()

        this.builder = this.makeBuilder()
        this.status = Status.STARTING

        return this
    }

    protected fun adaptContent(target: Button, id: Int): Button {
        if (this.content.isImage(id)) {
            target.text = ""
            target.background = this.content.getImage(id)
        } else {
            target.text = this.content.getSpanned(id)

            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(target, 24, 42, 1, TypedValue.COMPLEX_UNIT_SP)
            //target.textSize = 32f
            //target.setLineSpacing(0f, 1.5f)
            //target.setPadding(0, (target.textSize / 2 + 5).toInt(), 0, 0)

            target.minLines = 1
            target.maxLines = target.text.split(' ').count()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                target.breakStrategy = Layout.BREAK_STRATEGY_HIGH_QUALITY
                target.hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
            }
        }

        return target
    }

    protected fun stringMatch(a: String, b: String): Boolean {
        return a.toLowerCase().trim() == b.toLowerCase().trim()
    }

    private fun submitStatus() {
        if (!this.statusSubmitted) {
            this.statusSubmitted = true
            this.activity.answered(this.status)
            this.finally(this.status)
        }
    }

    protected fun submitStatus(status: Status) {
        this.status = status
        this.submitStatus()
    }

    protected fun setStatus(status: Status) {
        this.status = status
    }

    private fun settings() {
        this.activity.startSettingActivity()
    }

    protected open fun positive() {}
    protected open fun negative() {}
    protected open fun finally(status: Status) {}

    protected fun toast(text: CharSequence) {
        Toast.makeText(this.activity, text, Toast.LENGTH_SHORT).show()
    }

    private fun setLayout(@LayoutRes resource: Int): View {
        val inflater = this.activity.layoutInflater
        this.layout = inflater.inflate(resource, null)

        return this.layout
    }

    fun baseBuilder(): AlertDialog.Builder {
        return AlertDialog.Builder(this.activity)
            .setTitle(this.content.categoryName)
            .setNeutralButton("/") { _, _ -> this.settings() }
    }

    fun baseBuilder(textPositive: String): AlertDialog.Builder {
        return this.baseBuilder().setPositiveButton(textPositive) { _, _ -> this.positive() }
    }

    fun baseBuilder(@LayoutRes res: Int): AlertDialog.Builder {
        return this.baseBuilder().setView(this.setLayout(res))
    }

    fun baseBuilder(@LayoutRes res: Int, textPositive: String): AlertDialog.Builder {
        return this.baseBuilder(textPositive).setView(this.setLayout(res))
    }

    fun baseBuilder(textPositive: String, textNegative: String): AlertDialog.Builder {
        return this.baseBuilder(textPositive).setNegativeButton(textNegative) { _, _ -> this.negative() }
    }

    fun baseBuilder(@LayoutRes res: Int, textPositive: String, textNegative: String): AlertDialog.Builder {
        return this.baseBuilder(textPositive, textNegative).setView(this.setLayout(res))
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        this.submitStatus(if (this.status == Status.WAITING) Status.DISMISSED else this.status)
    }

    override fun onDestroy() {
        if (!this.statusSubmitted && this.status != Status.UNINITIALIZED)
            this.submitStatus(Status.DESTROYED)

        super.onDestroy()
    }

    companion object {

        enum class Status {
            UNINITIALIZED, // when the BaseCard has just been created
            STARTING, // when the content of the BaseCard is set and the builder made
            WAITING, // when creating the dialog (around the same time as the builder's create)
            SAVED, // when the card is destroyed after being saved
            DISMISSED, // when the user dismisses the BaseCard
            DESTROYED, // when the card seemed to have been destroyed by the system..?
            ANSWERED, ANSWERED_RIGHT, ANSWERED_WRONG // when the user answers
        }

    }

}
