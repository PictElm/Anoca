package com.patatos.sac.anoca

import android.content.*
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast

import com.patatos.sac.anoca.cards.BaseCard
import com.patatos.sac.anoca.cards.Content
import com.patatos.sac.anoca.cards.CustomCardParse
import com.patatos.sac.anoca.cards.data.DataCard
import com.patatos.sac.anoca.cards.data.MyRoomDatabase
import com.patatos.sac.anoca.cards.types.Associate
import com.patatos.sac.anoca.cards.types.Multiple
import com.patatos.sac.anoca.cards.types.TwoSided
import com.patatos.sac.anoca.cards.types.WritingTask

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private var finishCalled: Boolean = false

    private lateinit var sharedPref: SharedPreferences
    private lateinit var db: MyRoomDatabase

    private lateinit var data: List<DataCard>
    private var card: BaseCard? = null

    @Suppress("CascadeIf")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.sharedPref = this.getSharedPreferences(this.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        this.db = MyRoomDatabase.getInstance(this)

        try {
            if (savedInstanceState != null) {
                @Suppress("UNCHECKED_CAST")
                this.data = savedInstanceState.getParcelableArray(SAVED_DATA_KEY)!!.toList() as List<DataCard>
                this.card = this.supportFragmentManager.getFragment(
                    savedInstanceState,
                    SAVED_CARD_KEY
                ) as BaseCard?
            } else if (this.sharedPref.getBoolean(this.getString(R.string.master_switch_key), true)) {
                val extraIdRestriction = this.intent.getLongExtra(EXTRA_CAT_ID, -1)
                if (extraIdRestriction >= 0)
                    Log.i("anoca::test", "main activity started with restriction 'Category.id = $extraIdRestriction'")

                Executors.newSingleThreadExecutor().let {
                    it.execute {
                        this.card = this.randomCardType().also { cardType ->
                            cardType!!.setContent(
                                this.randomContent(cardType.contentSize ?: 1, extraIdRestriction)
                                    ?: return@also this.finish()
                            )
                            cardType.show(this.supportFragmentManager, "card")
                        }
                        it.shutdown()
                    }
                    if (!it.awaitTermination(20, TimeUnit.SECONDS)) this.finish()
                }
            } else this.finish()
        } catch (e: Exception) {
            val title = e.localizedMessage
            val text = Log.getStackTraceString(e)
            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(text)
                .setPositiveButton("Copy") { dialog, _ ->
                    val clipboard = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.primaryClip = ClipData.newPlainText(title, text)
                    Toast.makeText(this, "Error copied!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setNeutralButton("Pass") { dialog, _ -> dialog.dismiss() }
                .show()
            this.finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (this::data.isInitialized) {
            outState.putParcelableArray(SAVED_DATA_KEY, this.data.toTypedArray())
            this.supportFragmentManager.putFragment(outState, SAVED_CARD_KEY, this.card!!)
        } else this.finish()
    }

    override fun finish() {
        if (!this.finishCalled) {
            super.finish()
            this.finishAffinity()
            this.finishCalled = true
        }
    }

    private fun randomCardType(): BaseCard? {
        val cardTypes: MutableList<BaseCard> = mutableListOf()

        if (sharedPref.getBoolean(this.getString(R.string.switch_twosided_key), true))
            cardTypes.add(TwoSided())
        if (sharedPref.getBoolean(this.getString(R.string.switch_writingtask_key), true))
            cardTypes.add(WritingTask())
        if (sharedPref.getBoolean(this.getString(R.string.switch_multiple_key), true))
            cardTypes.add(Multiple())
        if (sharedPref.getBoolean(this.getString(R.string.switch_associate_key), true))
            cardTypes.add(Associate())

        return cardTypes.random()
    }

    private fun randomContent(n: Int, categoryIdRestriction: Long): Content? {
        val rawData: MutableList<String> = mutableListOf()

        val root = this.db.getDao().let {
            if (categoryIdRestriction < 0) it.randomCard()
            else it.randomCardCategoryRestriction(categoryIdRestriction)
        }.firstOrNull() ?: return null

        this.db.getDao().randomCards(root.id, root.categoryId!!, n - 1).let {
            this.data = listOf(root) + it
            this.data + List(n - it.count()) {
                DataCard(
                    this.getString(R.string.hint_front_text),
                    this.getString(R.string.hint_back_text)
                )
            }
        }.forEach {
            CustomCardParse.processNested(db, Pair(it.dataFRaw, it.dataBRaw)).apply {
                rawData.add(this.first)
                rawData.add(this.second)
            }
        }

        return Content(this, rawData, this.db.getDao().getCategory(root.categoryId!!).name)
    }

    fun answered(status: BaseCard.Companion.Status) {
        Log.i("anoca::answered", "$status")
        when (status) {
            BaseCard.Companion.Status.ANSWERED,
            BaseCard.Companion.Status.ANSWERED_RIGHT,
            BaseCard.Companion.Status.ANSWERED_WRONG -> {
                Executors.newSingleThreadExecutor().let { ex ->
                    ex.execute {
                        this.data.forEach {
                            val weight = this.db.getDao().firstWeights(it.id)[0]
                            weight.lastTime = System.currentTimeMillis()
                            this.db.getDao().updateWeights(weight)
                        }
                        this.data[0].also {
                            @Suppress("NON_EXHAUSTIVE_WHEN")
                            when (status) {
                                BaseCard.Companion.Status.ANSWERED_RIGHT -> it.answeredRight++
                                BaseCard.Companion.Status.ANSWERED_WRONG -> it.answeredWrong++
                            }
                            this.db.getDao().updateCards(it)
                        }
                        ex.shutdown()
                    }
                }
                this.finish()
            }
            //BaseCard.Companion.Status.SAVED, BaseCard.Companion.Status.DISMISSED
            else -> this.finish()
        }
    }

    fun startSettingActivity() {
        val activity = Intent(this, SettingActivity::class.java)
        this.startActivity(activity)
    }

    companion object {

        private const val SAVED_DATA_KEY = "saved-data"
        private const val SAVED_CARD_KEY = "saved-card"

        const val SAVED_CONTENT_KEY = "saved-card-content"
        const val SAVED_TITLE_KEY = "saved-category-name"

        const val EXTRA_CAT_ID = "com.patatos.sac.anoca.CAT_ID"

    }

}
