package com.patatos.sac.anoca.fragments

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter

import com.patatos.sac.anoca.cards.data.Category
import com.patatos.sac.anoca.cards.data.DataCard

class PagerAdapter(
    fragmentManager: FragmentManager,
    private val categories: List<Category>,
    selectCards: (category_id: Long) -> List<DataCard>,
    onEditCategory: (category: Category) -> Unit,
    onEditCard: (card: DataCard) -> Unit
) : FragmentStatePagerAdapter(fragmentManager) {

    private val categoryTabFragments: MutableList<CategoryTabFragment> = mutableListOf()

    init {
        this.categories.forEach {
            this.categoryTabFragments.add(CategoryTabFragment.newInstance(it, selectCards, onEditCategory, onEditCard))
        }
    }

    override fun getPageTitle(position: Int): String? {
        return this.categories[position].name
    }

    override fun getItem(position: Int): Fragment? {
        return this.categoryTabFragments[position]
    }

    override fun getCount(): Int {
        return this.categories.count()
    }

}
