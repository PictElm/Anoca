package com.patatos.sac.anoca

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.ArrayAdapter
import android.widget.Switch

import com.patatos.sac.anoca.cards.data.MyRoomDatabase
import com.patatos.sac.anoca.cards.data.DataCard
import com.patatos.sac.anoca.cards.data.Category
import com.patatos.sac.anoca.cards.data.Weight
import com.patatos.sac.anoca.fragments.PagerAdapter

import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.content_setting.*
import kotlinx.android.synthetic.main.dialog_edit_card.view.*
import kotlinx.android.synthetic.main.dialog_edit_category.view.*

import java.util.concurrent.Executors

class SettingActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var db: MyRoomDatabase

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.setContentView(R.layout.activity_setting)
        this.setSupportActionBar(toolbar)

        sharedPref = this.getSharedPreferences(this.getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        master_switch.isChecked = sharedPref.getBoolean(getString(R.string.master_switch_key), true)
        switch_twosided.isChecked = sharedPref.getBoolean(getString(R.string.switch_twosided_key), true)
        switch_writingtask.isChecked = sharedPref.getBoolean(getString(R.string.switch_writingtask_key), true)
        switch_multiple.isChecked = sharedPref.getBoolean(getString(R.string.switch_multiple_key), true)
        switch_associate.isChecked = sharedPref.getBoolean(getString(R.string.switch_associate_key), true)

        // setup settings toggles
        with(sharedPref.edit()) {
            master_switch.setOnClickListener {
                this.putBoolean(getString(R.string.master_switch_key), (it as Switch).isChecked.apply {
                    if (this)
                        this@SettingActivity.sendBroadcast(
                            Intent(this@SettingActivity, Receive::class.java)
                                .setAction(ACTION_SERVICE_DESTROYED)
                        )
                    else this@SettingActivity.stopService(
                            Intent(this@SettingActivity, MyService::class.java)
                        )
                }).apply()
            }
            switch_twosided.setOnClickListener {
                this.putBoolean(getString(R.string.switch_twosided_key), (it as Switch).isChecked).apply()
            }
            switch_writingtask.setOnClickListener {
                this.putBoolean(getString(R.string.switch_writingtask_key), (it as Switch).isChecked).apply()
            }
            switch_multiple.setOnClickListener {
                this.putBoolean(getString(R.string.switch_multiple_key), (it as Switch).isChecked).apply()
            }
            switch_associate.setOnClickListener {
                this.putBoolean(getString(R.string.switch_associate_key), (it as Switch).isChecked).apply()
            }
        }

        // async for db usage: setup categories and cards lists related fragments
        this.db = MyRoomDatabase.getInstance(this)

        Executors.newSingleThreadExecutor().let {
            it.execute {
                val allCategories = db.getDao().allCategories()

                // set dialog to add a category
                fab_add_category.setOnClickListener {
                    val layout = this.layoutInflater.inflate(R.layout.dialog_edit_category, null)

                    AlertDialog.Builder(this).setView(layout)
                        .setTitle(this.getString(R.string.add_category_title_text))
                        .setPositiveButton(R.string.edit_validate_text) { _, _ ->
                            Executors.newSingleThreadExecutor().let { ex ->
                                ex.execute {
                                    db.getDao().insertCategories(Category(
                                        layout.edit_name.text.toString().replace(Regex("[.?!]"), "_")
                                    ))

                                    this@SettingActivity.reloadLists()
                                    ex.shutdown()
                                }
                            }
                        }
                        .setNegativeButton(R.string.edit_cancel_text) { dialog, _ -> dialog.cancel() }
                        .create()
                        .show()
                }

                // set dialog to add a card
                fab_add_card.setOnClickListener {
                    val layout = this.layoutInflater.inflate(R.layout.dialog_edit_card, null)

                    ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        allCategories
                    ).also { adapter ->
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        layout.edit_category.adapter = adapter
                    }
                    layout.edit_category.setSelection(categories_pager.currentItem)

                    AlertDialog.Builder(this).setView(layout)
                        .setTitle(this.getString(R.string.add_card_title_text))
                        .setPositiveButton(R.string.edit_validate_text) { _, _ ->
                            Executors.newSingleThreadExecutor().let { ex ->
                                ex.execute {
                                    val w = layout.edit_weight.text.toString().toInt().let { w -> if(w < 0) 0 else w }
                                    val id = db.getDao().insertCards(
                                        DataCard(
                                            layout.edit_front.text.toString(),
                                            layout.edit_back.text.toString(),
                                            w,
                                            layout.edit_category.selectedItem as Category
                                        )
                                    )[0]
                                    db.getDao().insertWeights(List(w) { Weight(id) })

                                    this@SettingActivity.reloadLists()
                                    ex.shutdown()
                                }
                            }
                        }
                        .setNegativeButton(R.string.edit_cancel_text) { dialog, _ -> dialog.cancel() }
                        .create()
                        .show()
                }

                val selectCards = { categoryId: Long -> db.getDao().allCards(categoryId) }
                val onEditCategory = { category: Category ->
                    val layout = this.layoutInflater.inflate(R.layout.dialog_edit_category, null)

                    // set elements to edit
                    layout.edit_name.setText(category.name)
                    layout.switch_category.isChecked = category.enabled

                    // set buttons to: apply changes, deny changes, delete category
                    AlertDialog.Builder(this).setView(layout)
                        .setTitle(this.getString(R.string.edit_category_title_text))
                        .setPositiveButton(R.string.edit_validate_text) { _, _ ->
                            Executors.newSingleThreadExecutor().let { ex ->
                                ex.execute {
                                    category.name = layout.edit_name.text.toString().replace(Regex("[.?!]"), "_")
                                    category.enabled = layout.switch_category.isChecked
                                    db.getDao().updateCategories(category)

                                    this@SettingActivity.reloadLists()
                                    ex.shutdown()
                                }
                            }
                        }
                        .setNegativeButton(R.string.edit_cancel_text) { dialog, _ -> dialog.cancel() }
                        .setNeutralButton(R.string.edit_delete_text) { _, _ ->
                            Executors.newSingleThreadExecutor().let { ex ->
                                ex.execute {
                                    db.getDao().deleteCategories(category)

                                    this@SettingActivity.reloadLists()
                                    ex.shutdown()
                                }
                            }
                        }
                        .create()
                        .show()
                }
                val onEditCard = { card: DataCard ->
                    val layout = this.layoutInflater.inflate(R.layout.dialog_edit_card, null)

                    // set text to edit
                    layout.edit_front.setText(card.dataFRaw)
                    layout.edit_back.setText(card.dataBRaw)
                    layout.switch_canincluded.isChecked = card.canIncluded
                    layout.edit_weight.setText(card.weight.toString())

                    // set category to edit
                    ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        allCategories
                    ).also { adapter ->
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        layout.edit_category.adapter = adapter
                    }
                    layout.edit_category.setSelection(categories_pager.currentItem)

                    // set buttons to: apply changes, deny changes, delete card
                    AlertDialog.Builder(this).setView(layout)
                        .setTitle(this.getString(R.string.edit_card_title_text))
                        .setPositiveButton(R.string.edit_validate_text) { _, _ ->
                            Executors.newSingleThreadExecutor().let { ex ->
                                ex.execute {
                                    val w = layout.edit_weight.text.toString().toInt().let { w -> if(w < 0) 0 else w }
                                    var toAdd = w - card.weight
                                    if (toAdd < 0) {
                                        db.getDao().deleteWeights(db.getDao().firstWeights(card.id, card.weight))
                                        toAdd = w
                                    }

                                    card.dataFRaw = layout.edit_front.text.toString()
                                    card.dataBRaw = layout.edit_back.text.toString()
                                    card.canIncluded = layout.switch_canincluded.isChecked
                                    card.weight = w
                                    card.categoryId = (layout.edit_category.selectedItem as Category).id
                                    db.getDao().updateCards(card)

                                    if (0 < toAdd)
                                        db.getDao().insertWeights(List(toAdd) { Weight(card.id) })

                                    this@SettingActivity.reloadLists()
                                    ex.shutdown()
                                }
                            }
                        }
                        .setNegativeButton(R.string.edit_cancel_text) { dialog, _ -> dialog.cancel() }
                        .setNeutralButton(R.string.edit_delete_text) { _, _ ->
                            Executors.newSingleThreadExecutor().let { ex ->
                                ex.execute {
                                    db.getDao().deleteCards(card)

                                    this@SettingActivity.reloadLists()
                                    ex.shutdown()
                                }
                            }
                        }
                        .create()
                        .show()
                }

                categories_pager.adapter = PagerAdapter(
                    this.supportFragmentManager,
                    allCategories,
                    selectCards,
                    onEditCategory,
                    onEditCard
                )
                categories_pager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(categories_tabs))
                categories_tabs.setupWithViewPager(categories_pager)

                it.shutdown()
            }
        }
    }

    override fun onPause() {
        if (sharedPref.getBoolean(getString(R.string.master_switch_key), true))
            this.startService(Intent(this, MyService::class.java))

        super.onPause()
    }

    private fun reloadLists() {
        //categories_pager.visibility = View.GONE
        categories_pager.postInvalidateDelayed(500)
        //categories_pager.visibility = View.VISIBLE
    }

}
