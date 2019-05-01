package com.patatos.sac.anoca.fragments

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast

import com.patatos.sac.anoca.R
import com.patatos.sac.anoca.cards.data.DataCard

import kotlinx.android.synthetic.main.fragment_card.view.*

class MyCardRecyclerViewAdapter(
        private val context: Context,
        private val cards: List<DataCard>?,
        private val onEditCard: (card: DataCard) -> Unit
    ) : RecyclerView.Adapter<MyCardRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = cards!![position]

        holder.dataFButton.text = item.getSimplifiedDataF()
        holder.dataBButton.text = item.getSimplifiedDataB()

        holder.editButton.setOnClickListener { this.onEditCard(this.cards[position]) }
        holder.editButton.setOnLongClickListener {
            Toast.makeText(
                this.context,
                //"${DateFormat.getInstance().format(item.lastTime)} - ${item.counter}/${item.weight}",
                "right: ${item.answeredRight} / wrong: ${item.answeredWrong} (weight: ${item.weight})",
                Toast.LENGTH_LONG
            ).show()
            true
        }
    }

    override fun getItemCount(): Int {
        return cards?.count() ?: 0
    }

    inner class ViewHolder(cardView: View) : RecyclerView.ViewHolder(cardView) {
        val dataFButton: Button = cardView.data_f
        val dataBButton: Button = cardView.data_b
        val editButton: Button = cardView.button_edit_card
    }

}
