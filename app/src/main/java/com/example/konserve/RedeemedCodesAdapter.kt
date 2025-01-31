package com.example.konserve

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RedeemedCodesAdapter(private val redeemedCodes: List<String>) :
    RecyclerView.Adapter<RedeemedCodesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val codeTextView: TextView = view.findViewById(R.id.redeemedCodeTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_redeemed_code, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.codeTextView.text = redeemedCodes[position]
    }

    override fun getItemCount() = redeemedCodes.size
}
