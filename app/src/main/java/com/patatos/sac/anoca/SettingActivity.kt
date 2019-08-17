package com.patatos.sac.anoca

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*

import com.patatos.sac.anoca.cards.data.Category
import com.patatos.sac.anoca.cards.data.DataCard
import com.patatos.sac.anoca.cards.data.MyRoomDatabase
import com.patatos.sac.anoca.cards.data.Weight
import com.patatos.sac.anoca.fragments.PagerAdapter
import io.github.mljli.rubyspan.RubyTagHandler

import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.content_setting.*
import kotlinx.android.synthetic.main.dialog_edit_card.view.*
import kotlinx.android.synthetic.main.dialog_edit_category.view.*

import lib.folderpicker.FolderPicker

import java.io.File
import java.util.concurrent.Executors

@SuppressLint("InflateParams")
class SettingActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var db: MyRoomDatabase

    private var savedLastTab = 0
    private var isPagerInitialized = false

    private lateinit var allCategories: List<Category>

    private var shouldReloadLists = true

    private var onFolderSelected: ((String) -> Unit)? = null
    private var onPermissionsAnswered: ((Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.setContentView(R.layout.activity_setting)
        this.setSupportActionBar(toolbar)

        this.sharedPref = this.getSharedPreferences(this.getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        this.db = MyRoomDatabase.getInstance(this)

        this.savedLastTab = this.sharedPref.getInt(this.getString(R.string.last_tab_key), 0)

        // set settings toggles
        this.sharedPref.edit().also { editor ->
            listOf(
                Pair(master_switch, R.string.master_switch_key),
                Pair(switch_twosided, R.string.switch_twosided_key),
                Pair(switch_writingtask, R.string.switch_writingtask_key),
                Pair(switch_multiple, R.string.switch_multiple_key),
                Pair(switch_associate, R.string.switch_associate_key)
            ).forEach { p ->
                val key = this.getString(p.second)
                p.first.isChecked = this.sharedPref.getBoolean(key, true)
                p.first.setOnClickListener { editor.putBoolean(key, p.first.isChecked).apply() }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (this.shouldReloadLists) {
            this.reloadLists()
            this.shouldReloadLists = false
        }
    }

    override fun onPause() {
        super.onPause()

        this.savedLastTab = categories_pager.currentItem
        this.sharedPref.edit().putInt(this.getString(R.string.last_tab_key), this.savedLastTab).apply()
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE)
            this.onPermissionsAnswered!!(grantResults.all { it == PackageManager.PERMISSION_GRANTED })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FOLDER_REQUEST_CODE && resultCode == Activity.RESULT_OK)
            this.onFolderSelected!!(data?.extras?.getString("data")!!)
    }

    private fun notifyUser(text: String, vararg args: Any) {
        this.runOnUiThread { Toast.makeText(this, text.format(*args), Toast.LENGTH_LONG).show() }
    }

    private fun setupAddCategory() {
        fab_add_category.setOnClickListener {
            val layout = this.layoutInflater.inflate(R.layout.dialog_edit_category, null)

            AlertDialog.Builder(this).setView(layout)
                .setTitle(this.getString(R.string.add_category_title_text))
                .setPositiveButton(R.string.edit_validate_text) { _, _ ->
                    this.updateListsAfter {
                        this.db.getDao().insertCategories(
                            Category(
                                layout.edit_name.text.toString().replace(Regex("[.?!]"), "_")
                            )
                        )
                    }
                }
                .setNegativeButton(R.string.edit_cancel_text) { dialog, _ -> dialog.cancel() }
                .create()
                .show()
        }
    }

    private fun setupAddCard() {
        fab_add_card.setOnClickListener {
            val layout = this.layoutInflater.inflate(R.layout.dialog_edit_card, null)

            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                this.allCategories
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                layout.edit_category.adapter = adapter
            }
            layout.edit_category.setSelection(categories_pager.currentItem)

            AlertDialog.Builder(this).setView(layout)
                .setTitle(this.getString(R.string.add_card_title_text))
                .setPositiveButton(R.string.edit_validate_text) { _, _ ->
                    this.updateListsAfter {
                        val w = layout.edit_weight.text.toString().toInt().let { w -> if (w < 0) 0 else w }
                        val id = this.db.getDao().insertCards(
                            DataCard(
                                layout.edit_front.text.toString(),
                                layout.edit_back.text.toString(),
                                w,
                                layout.edit_category.selectedItem as Category
                            )
                        )[0]
                        this.db.getDao().insertWeights(List(w) { Weight(id) })
                    }
                }
                .setNegativeButton(R.string.edit_cancel_text) { dialog, _ -> dialog.cancel() }
                .create()
                .show()
        }
    }

    private fun setupFloatingButtons() {
        // set dialog to add a category
        this.setupAddCategory()

        // set dialog to add a card
        this.setupAddCard()

        val debugMessageLeft = this.db.getDao().allWeights().joinToString("\n") { w -> w.toString() }
        fab_add_category.setOnLongClickListener {
            AlertDialog.Builder(this)
                .setMessage(debugMessageLeft)
                .setPositiveButton(R.string.edit_validate_text) { dialog, _ -> dialog.dismiss() }
                .setCancelable(true)
                .create().show()
            true
        }

        val debugMessageRight = "<p><ruby>日本<rt>にっぽん</rt></ruby>!!</p>"
        fab_add_card.setOnLongClickListener {
            val b = TextView(this)
            b.textSize = 20f
            b.setLineSpacing(0f, 1.5f)
            b.setPadding(0, b.textSize.toInt(), 0, -2 * b.textSize.toInt())
            b.text = Html.fromHtml(debugMessageRight, 0, null, RubyTagHandler())

            AlertDialog.Builder(this)
                .setView(b)
                .setPositiveButton(R.string.edit_validate_text) { dialog, _ -> dialog.dismiss() }
                .setCancelable(true)
                .create().show()
            true
        }
    }

    private fun setupListsPager(): PagerAdapter {
        this.allCategories = this.db.getDao().allCategories()

        // set right and left floating action buttons
        this.setupFloatingButtons()

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
                    this.updateListsAfter {
                        category.name = layout.edit_name.text.toString().replace(Regex("[.?!]"), "_")
                        category.enabled = layout.switch_category.isChecked
                        this.db.getDao().updateCategories(category)
                    }
                }
                .setNegativeButton(R.string.edit_cancel_text) { dialog, _ -> dialog.cancel() }
                .setNeutralButton(R.string.edit_delete_text) { _, _ ->
                    this.updateListsAfter {
                        this.db.getDao().deleteCardsFromCategory(category.id)
                        this.db.getDao().deleteCategories(category)
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
            layout.edit_category.setSelection(categories_pager.currentItem, true)

            // set buttons to: apply changes, deny changes, delete card
            AlertDialog.Builder(this)
                .setView(layout)
                .setTitle(this.getString(R.string.edit_card_title_text))
                .setPositiveButton(R.string.edit_validate_text) { _, _ ->
                    this.updateListsAfter {
                        val w = layout.edit_weight.text.toString().toInt().let { w -> if (w < 0) 0 else w }
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
                    }
                }
                .setNegativeButton(R.string.edit_cancel_text) { dialog, _ -> dialog.cancel() }
                .setNeutralButton(R.string.edit_delete_text) { _, _ ->
                    this.updateListsAfter {
                        this.db.getDao().deleteCards(card)
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

    private fun askPermissions(vararg permissions: String, then: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notGranted = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (notGranted.isNotEmpty()) {
                this.requestPermissions(notGranted.toTypedArray(), PERMISSION_REQUEST_CODE)
                this.onPermissionsAnswered = then
            } else then(true)
        } else then(true)
    }

    private fun askLocation(title: String, pickFiles: Boolean, then: (String) -> Unit) {
        this.askPermissions(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) { granted ->
            if (granted) {
                this.startActivityForResult(
                    Intent(this, FolderPicker::class.java)
                        .putExtra("title", title)
                        .putExtra("pickFiles", pickFiles),
                    FOLDER_REQUEST_CODE
                )
                this.onFolderSelected = then
            } else this.notifyUser(this.getString(R.string.permission_required_text))
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
        this.askLocation(this.getString(R.string.save_title_text), false) { folder ->
            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_TEXT
            input.setHint(R.string.default_file_name)
            AlertDialog.Builder(this)
                .setTitle(this.getText(R.string.confirm_action_save_text))
                .setView(input)
                .setNegativeButton(this.getText(R.string.confirm_negative_text)) { dialog, _ -> dialog.dismiss() }
                .setPositiveButton(this.getText(R.string.confirm_positive_text)) { _, _ ->
                    this.updateListsAfter {
                        var counter = 0
                        this.db.getDao().also {
                            this.writeFile(
                                File(folder, input.text.toString()),
                                this.allCategories.joinToString("\n$CAT_SEP\n") { category ->
                                    "${category.name}\n" + it.allCards(category.id).joinToString("\n") { card ->
                                        counter++
                                        "${card.dataFRaw}$DAT_SEP${card.dataBRaw}$DAT_SEP${card.weight}"
                                    }
                                }
                            )
                        }
                        this.notifyUser(this.getString(R.string.confirm_done_save_text), counter)
                    }
                }
                .show()
        }
    }

    private fun actionLoad() {
        this.askLocation(this.getString(R.string.load_title_text), true) { filepath ->
            this.updateListsAfter {
                var counter = 0
                this.db.getDao().also {
                    this.readFile(File(filepath)).split("$CAT_SEP\n").forEach { data ->
                        var categoryId: Long = -1
                        var categoryName: String? = null
                        data.split("\n").forEach { line ->
                            if (categoryId < 0) {
                                categoryName = line
                                categoryId = it.insertCategories(Category(0, categoryName!!, true))[0]
                            } else if (line != "") {
                                counter++
                                val desc = line.split(DAT_SEP)
                                val w = desc[2].toInt()
                                val id = it.insertCards(
                                    DataCard(
                                        0, desc[0], desc[1],
                                        0, 0,
                                        true, w,
                                        categoryId, categoryName
                                    )
                                )[0]
                                it.insertWeights(List(w) { Weight(id) })
                            }
                        }
                    }
                }
                this.notifyUser(this.getString(R.string.confirm_done_load_text), counter)
            }
        }
    }

    private fun actionClear() {
        AlertDialog.Builder(this)
            .setTitle(this.getText(R.string.confirm_action_clear_text))
            .setNegativeButton(this.getText(R.string.confirm_negative_text)) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(this.getText(R.string.confirm_positive_text)) { _, _ ->
                this.updateListsAfter {
                    var counter = 0
                    this.db.getDao().also {
                        counter += it.allCards().count()
                        it.clearCards()
                        it.clearCategories()
                        it.clearWeights()
                    }
                    this.notifyUser(this.getString(R.string.confirm_done_clear_text), counter)
                }
            }
            .show()
    }

    private fun updateListsAfter(action: (() -> Unit)? = null) {
        this.shouldReloadLists = true

        Executors.newSingleThreadExecutor().let {
            it.execute {
                action?.invoke()

                this.setupListsPager().also { pager ->
                    this.runOnUiThread {
                        if (this.isPagerInitialized)
                            this.savedLastTab = categories_pager.currentItem
                        else this.isPagerInitialized = true

                        categories_pager.adapter = pager
                        categories_pager.addOnPageChangeListener(
                            TabLayout.TabLayoutOnPageChangeListener(categories_tabs)
                        )
                        categories_tabs.setupWithViewPager(categories_pager)
                        categories_pager.setCurrentItem(this.savedLastTab, false)

                        Log.i("anoca::reloaded", categories_pager.toString())
                    }
                }

                it.shutdown()
            }
        }
    }

    private fun reloadLists() = this.updateListsAfter()

    companion object {

        const val FOLDER_REQUEST_CODE = 42
        const val PERMISSION_REQUEST_CODE = 42

        const val CAT_SEP = "---"
        const val DAT_SEP = "\\\\"

    }

}
