package com.example.konserve

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Memo(val title: String, val content: String, val timestamp: Long)


class MemoAdapter : RecyclerView.Adapter<MemoAdapter.MemoViewHolder>() {

    private var memos: List<Memo> = listOf()

    inner class MemoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val memoTitleView: TextView = itemView.findViewById(R.id.memoTitle)
        private val memoLocationView: TextView = itemView.findViewById(R.id.memoLocation)
        private val memoTimeView: TextView = itemView.findViewById(R.id.memoTime)

        fun bind(memo: Memo) {
            memoTitleView.text = memo.title
            memoLocationView.text = memo.content

            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val date = Date(memo.timestamp)
            memoTimeView.text = sdf.format(date)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.memo_item, parent, false)
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
        this.memos = ArrayList(newMemos)


        diffResult.dispatchUpdatesTo(this)
    }

    class MemoDiffCallback(
        private val oldList: List<Memo>,
        private val newList: List<Memo>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].timestamp == newList[newItemPosition].timestamp
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}


