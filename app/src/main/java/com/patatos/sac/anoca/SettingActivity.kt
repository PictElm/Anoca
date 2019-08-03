package com.patatos.sac.anoca

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast

import com.patatos.sac.anoca.cards.data.Category
import com.patatos.sac.anoca.cards.data.DataCard
import com.patatos.sac.anoca.cards.data.MyRoomDatabase
import com.patatos.sac.anoca.cards.data.Weight
import com.patatos.sac.anoca.cards.data.csv.Csvable
import com.patatos.sac.anoca.fragments.PagerAdapter

import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.content_setting.*
import kotlinx.android.synthetic.main.dialog_edit_card.view.*
import kotlinx.android.synthetic.main.dialog_edit_category.view.*

import java.io.File
import java.util.concurrent.Executors

@SuppressLint("InflateParams")
class SettingActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var db: MyRoomDatabase

    private var onFolderSelected: ((Any) -> Unit)? = null

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
                this.putBoolean(getString(R.string.master_switch_key), (it as Switch).isChecked).apply()
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
        this.updateLists()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menuInflater.inflate(R.menu.menu_setting, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_save -> this.actionSave()
            R.id.action_load -> this.actionLoad()
            R.id.action_clear -> this.actionClear()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        this.finish()
        super.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK)
            this.onFolderSelected!!(data?.data!!)
    }

    private fun setupAddCategory() {
        fab_add_category.setOnClickListener {
            val layout = this.layoutInflater.inflate(R.layout.dialog_edit_category, null)

            AlertDialog.Builder(this).setView(layout)
                .setTitle(this.getString(R.string.add_category_title_text))
                .setPositiveButton(R.string.edit_validate_text) { _, _ ->
                    Executors.newSingleThreadExecutor().let { ex ->
                        ex.execute {
                            this.db.getDao().insertCategories(Category(
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
    }

    private fun setupAddCard(allCategories: List<Category>) {
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
                            val id = this.db.getDao().insertCards(
                                DataCard(
                                    layout.edit_front.text.toString(),
                                    layout.edit_back.text.toString(),
                                    w,
                                    layout.edit_category.selectedItem as Category
                                )
                            )[0]
                            this.db.getDao().insertWeights(List(w) { Weight(id) })

                            this@SettingActivity.reloadLists()
                            ex.shutdown()
                        }
                    }
                }
                .setNegativeButton(R.string.edit_cancel_text) { dialog, _ -> dialog.cancel() }
                .create()
                .show()
        }
    }

    private fun setupListsPager(): PagerAdapter {
        val allCategories = this.db.getDao().allCategories()

        // set dialog to add a category
        this.setupAddCategory()

        // set dialog to add a card
        this.setupAddCard(allCategories)

        val debugMessageLeft = this.db.getDao().allWeights().fold("") { c, w -> "$c\n$w" }
        fab_add_category.setOnLongClickListener {
            AlertDialog.Builder(this)
                .setMessage(debugMessageLeft)
                .setPositiveButton(R.string.edit_validate_text) { dialog, _ -> dialog.dismiss() }
                .setCancelable(true)
                .create().show()
            true
        }

        val selectCards = { categoryId: Long -> this.db.getDao().allCards(categoryId) }
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
                            this.db.getDao().updateCategories(category)

                            this@SettingActivity.reloadLists()
                            ex.shutdown()
                        }
                    }
                }
                .setNegativeButton(R.string.edit_cancel_text) { dialog, _ -> dialog.cancel() }
                .setNeutralButton(R.string.edit_delete_text) { _, _ ->
                    Executors.newSingleThreadExecutor().let { ex ->
                        ex.execute {
                            this.db.getDao().deleteCardsFromCategory(category.id)
                            this.db.getDao().deleteCategories(category)

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
                                this.db.getDao().deleteWeights(this.db.getDao().firstWeights(card.id, card.weight))
                                toAdd = w
                            }

                            card.dataFRaw = layout.edit_front.text.toString()
                            card.dataBRaw = layout.edit_back.text.toString()
                            card.canIncluded = layout.switch_canincluded.isChecked
                            card.weight = w
                            card.categoryId = (layout.edit_category.selectedItem as Category).id
                            this.db.getDao().updateCards(card)

                            if (0 < toAdd)
                                this.db.getDao().insertWeights(List(toAdd) { Weight(card.id) })

                            this@SettingActivity.reloadLists()
                            ex.shutdown()
                        }
                    }
                }
                .setNegativeButton(R.string.edit_cancel_text) { dialog, _ -> dialog.cancel() }
                .setNeutralButton(R.string.edit_delete_text) { _, _ ->
                    Executors.newSingleThreadExecutor().let { ex ->
                        ex.execute {
                            this.db.getDao().deleteCards(card)

                            this@SettingActivity.reloadLists()
                            ex.shutdown()
                        }
                    }
                }
                .create()
                .show()
        }

        return PagerAdapter(
            this.supportFragmentManager,
            allCategories,
            selectCards,
            onEditCategory,
            onEditCard
        )
    }

    private fun askFolder(function: (String) -> Unit) {
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), READ_REQUEST_CODE)
            this.onFolderSelected = function
        } else*/
        EditText(this).also {
            it.inputType = InputType.TYPE_CLASS_TEXT
            AlertDialog.Builder(this)
                .setTitle(this.getString(R.string.confirm_action_folder_text))
                .setView(it)
                .setPositiveButton(this.getText(R.string.confirm_positive_text)) { _, _ -> function(it.text.toString()) }
                .show()
        }
    }

    private fun writeFile(file: File, data: String) {
        file.createNewFile()
        file.writer().also {
            it.write(data)
            it.close()
        }
    }

    private fun readFile(file: File): String {
        return file.reader().let {
            val r = it.readText()
            it.close()
            r
        }
    }

    private fun actionSave() {
        this.askFolder { folder ->
            Executors.newSingleThreadExecutor().let { ex ->
                ex.execute {
                    this.db.getDao().also {
                        this.writeFile(File(folder, this.getString(R.string.csv_cards_file)), Csvable.csvAll(";", "\"", it.getCards()))
                        this.writeFile(File(folder, this.getString(R.string.csv_categories_file)), Csvable.csvAll(";", "\"", it.getCategories()))
                        this.writeFile(File(folder, this.getString(R.string.csv_weights_file)), Csvable.csvAll(";", "\"", it.getWeights()))
                    }
                    this.runOnUiThread { Toast.makeText(this, this.getString(R.string.confirm_done_text), Toast.LENGTH_LONG).show() }
                    this.reloadLists()
                    ex.shutdown()
                }
            }
        }
    }

    private fun actionLoad() {
        this.askFolder { folder ->
            Executors.newSingleThreadExecutor().let { ex ->
                ex.execute {
                    this.db.getDao().also {
                        it.setCards(this.readFile(File(folder, this.getString(R.string.csv_cards_file))).split("\n").let { l -> List(l.count() - 1) { k -> DataCard.fromCsv(";", "\"", l[k]) } } )
                        it.setCategories(this.readFile(File(folder, this.getString(R.string.csv_categories_file))).split("\n").let { l -> List(l.count() - 1) { k -> Category.fromCsv(";", "\"", l[k]) } } )
                        it.setWeights(this.readFile(File(folder, this.getString(R.string.csv_weights_file))).split("\n").let { l -> List(l.count() - 1) { k -> Weight.fromCsv(";", "\"", l[k]) } } )
                    }
                    this.runOnUiThread { Toast.makeText(this, this.getString(R.string.confirm_done_text), Toast.LENGTH_LONG).show() }
                    this.reloadLists()
                    ex.shutdown()
                }
            }
        }
    }

    private fun actionClear() {
        AlertDialog.Builder(this)
            .setTitle(this.getText(R.string.confirm_action_clear_text))
            .setNegativeButton(this.getText(R.string.confirm_negative_text)) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(this.getText(R.string.confirm_positive_text)) { _, _ ->
                Executors.newSingleThreadExecutor().let { ex ->
                    ex.execute {
                        this.db.getDao().also {
                            it.clearCards()
                            it.clearCategories()
                            it.clearWeights()
                        }
                        this.runOnUiThread { Toast.makeText(this, this.getString(R.string.confirm_done_text), Toast.LENGTH_LONG).show() }
                        this.reloadLists()
                        ex.shutdown()
                    }
                }
            }
            .show()
    }

    private fun updateLists() {
        this.db = MyRoomDatabase.getInstance(this)

        Executors.newSingleThreadExecutor().let {
            it.execute {
                this.setupListsPager().also { pager ->
                    this.runOnUiThread {
                        categories_pager.adapter = pager
                        categories_pager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(categories_tabs))
                        categories_tabs.setupWithViewPager(categories_pager)
                    }
                }
                it.shutdown()
            }
        }
    }

    private fun reloadLists() {
        this.runOnUiThread {
            categories_pager.visibility = View.GONE
            categories_pager.postInvalidate()
            this.updateLists()
            categories_pager.visibility = View.VISIBLE

            Log.i("anoca::reloaded", categories_pager.toString())
        }
    }

    companion object {
        const val READ_REQUEST_CODE = 42
    }

}
