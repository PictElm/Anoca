package com.patatos.sac.anoca

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log

import com.patatos.sac.anoca.cards.*
import com.patatos.sac.anoca.cards.data.MyRoomDatabase
import com.patatos.sac.anoca.cards.data.DataCard
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
    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.sharedPref = this.getSharedPreferences(this.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        this.db = MyRoomDatabase.getInstance(this)

        if (savedInstanceState != null) {
            @Suppress("UNCHECKED_CAST")
            this.data = savedInstanceState.getParcelableArray(SAVED_DATA_KEY)!!.toList() as List<DataCard>
            this.card = this.supportFragmentManager.getFragment(
                savedInstanceState,
                SAVED_CARD_KEY
            ) as BaseCard?
        } else if (this.sharedPref.getBoolean(this.getString(R.string.master_switch_key), true))
            Executors.newSingleThreadExecutor().let {
                it.execute {
                    this.card = this.randomCard().also { card ->
                        card!!.setContent(this.randomContent(card.contentSize ?: 1))
                        card.show(this.supportFragmentManager, "card")
                    }
                    it.shutdown()
                }
                if (!it.awaitTermination(20, TimeUnit.SECONDS)) this.finish()
            }
        else this.finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelableArray(SAVED_DATA_KEY, this.data.toTypedArray())
        this.supportFragmentManager.putFragment(outState, SAVED_CARD_KEY, this.card!!)
    }

    override fun finish() {
        if (!this.finishCalled) {
            super.finish()
            this.finishAffinity()
            this.finishCalled = true
        }
    }

    private fun randomCard(): BaseCard? {
        val cards: MutableList<BaseCard> = mutableListOf()

        if (sharedPref.getBoolean(this.getString(R.string.switch_twosided_key), true))
            cards.add(TwoSided())
        if (sharedPref.getBoolean(this.getString(R.string.switch_writingtask_key), true))
            cards.add(WritingTask())
        if (sharedPref.getBoolean(this.getString(R.string.switch_multiple_key), true))
            cards.add(Multiple())
        if (sharedPref.getBoolean(this.getString(R.string.switch_associate_key), true))
            cards.add(Associate())

        return cards.random()
    }

    private fun randomContent(n: Int): Content {
        val rawData: MutableList<String> = mutableListOf()

        val root = this.db.getDao().randomCard()[0]
        this.db.getDao().randomCards(root.id, root.categoryId!!,n - 1).let {
            this.data = listOf(root) + it
            this.data + List(n - it.count()) { DataCard(this.getString(R.string.hint_front_text), this.getString(R.string.hint_back_text)) }
        }.forEach {
            CustomCardParse.processNested(db, Pair(it.dataFRaw, it.dataBRaw)).apply {
                rawData.add(this.first)
                rawData.add(this.second)
            }
        }

        return Content(this, rawData)
    }

    fun answered(status: Status) {
        Log.i("anoca::answered", "$status")
        if (status == Status.ANSWERED || status == Status.ANSWERED_RIGHT || status == Status.ANSWERED_WRONG) {
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
                            Status.ANSWERED_RIGHT -> it.answeredRight++
                            Status.ANSWERED_WRONG -> it.answeredWrong++
                        }
                        this.db.getDao().updateCards(it)
                    }
                    ex.shutdown()
                }
            }
            this.finish()
        } else if (status == Status.SAVED || status == Status.DISMISSED) this.finish()
    }

    fun startSettingActivity() {
        val activity = Intent(this, SettingActivity::class.java)
        this.startActivity(activity)
    }

    companion object {

        private const val SAVED_DATA_KEY = "saved-data"
        private const val SAVED_CARD_KEY = "saved-card"
        const val SAVED_CONTENT_KEY = "saved-card-content"

    }

}
