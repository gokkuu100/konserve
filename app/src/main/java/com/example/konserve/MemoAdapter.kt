package com.example.konserve

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

data class Memo(val text: String) // Example data class

class MemoAdapter : RecyclerView.Adapter<MemoAdapter.MemoViewHolder>() {

    private var memos: List<Memo> = listOf()

    inner class MemoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val memoTextView: TextView = itemView.findViewById(R.id.memoTextView) // Update this with your actual TextView ID

        fun bind(memo: Memo) {
            memoTextView.text = memo.text
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.memo_item, parent, false) // Update with your item layout
        return MemoViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemoViewHolder, position: Int) {
        val memo = memos[position]
        holder.bind(memo)
    }

    override fun getItemCount(): Int {
        return memos.size
    }

    fun setMemos(newMemos: List<Memo>) {
        val diffCallback = MemoDiffCallback(memos, newMemos)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        memos = newMemos
        diffResult.dispatchUpdatesTo(this)
    }

    class MemoDiffCallback(
        private val oldList: List<Memo>,
        private val newList: List<Memo>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            // Assuming Memo has a unique identifier, like an ID
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
