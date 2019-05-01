package com.patatos.sac.anoca.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.patatos.sac.anoca.R
import com.patatos.sac.anoca.cards.data.DataCard

class CardFragment : Fragment() {

    private var dataCards: List<DataCard>? = null
    private lateinit var onEditCard: (card: DataCard) -> Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.arguments?.let {
            @Suppress("UNCHECKED_CAST")
            this.dataCards = it.getParcelableArray(ARG_DATA_CARDS)!!.toList() as List<DataCard>
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_card_list, container, false)

        with(view as RecyclerView) {
            this.layoutManager = LinearLayoutManager(context)
            this.adapter = MyCardRecyclerViewAdapter(this@CardFragment.context!!, dataCards, onEditCard)
        }
        return view
    }

    fun setCards(cards: List<DataCard>, onEditCard: (card: DataCard) -> Unit) {
        this.dataCards = cards
        this.arguments = Bundle().apply {
            putParcelableArray(ARG_DATA_CARDS, dataCards!!.toTypedArray())
        }
        this.onEditCard = onEditCard
    }

    companion object {

        const val ARG_DATA_CARDS = "data-cards"

        @JvmStatic
        fun newInstance(dataCards: List<DataCard>, onEditCard: (card: DataCard) -> Unit): CardFragment {
            return CardFragment().apply {
                this.arguments = Bundle().apply {
                    putParcelableArray(ARG_DATA_CARDS, dataCards.toTypedArray())
                }
                this.onEditCard = onEditCard
            }
        }

    }

}
