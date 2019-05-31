package com.patatos.sac.anoca.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.patatos.sac.anoca.R
import com.patatos.sac.anoca.cards.data.Category
import com.patatos.sac.anoca.cards.data.DataCard

import kotlinx.android.synthetic.main.tab_fragment_card_list.view.*

class CategoryTabFragment : Fragment() {

    private lateinit var category: Category
    private lateinit var cards: List<DataCard>

    private lateinit var onEditCategory: (category: Category) -> Unit
    private lateinit var onEditCard: (card: DataCard) -> Unit

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.tab_fragment_card_list, container, false)

        view.button_edit_category.setOnClickListener { onEditCategory(this.category) }

        return view
    }

    override fun onAttachFragment(childFragment: Fragment?) {
        with(childFragment!! as CardFragment) { this@with.setCards(cards, onEditCard) }
        super.onAttachFragment(childFragment)
    }

    companion object {

        @JvmStatic
        fun newInstance(
                    category: Category,
                    selectCards: (category_id: Long) -> List<DataCard>,
                    onEditCategory: (category: Category) -> Unit,
                    onEditCard: (card: DataCard) -> Unit
                ): CategoryTabFragment {
            return CategoryTabFragment().apply {
                this.category = category
                this.cards = selectCards(category.id)

                this.onEditCategory = onEditCategory
                this.onEditCard = onEditCard
            }
        }

    }

}
