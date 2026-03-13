package com.onbada.seathermo.presentation.point.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.onbada.seathermo.R
import com.onbada.seathermo.domain.entity.PointDate

/**
 * 포인트 날짜 목록 어댑터
 * Point Date List Adapter
 */
class PointDateAdapter(
    private val onItemClick: (PointDate) -> Unit
) : ListAdapter<PointDate, PointDateAdapter.PointDateViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PointDateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_point_date, parent, false)
        return PointDateViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: PointDateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PointDateViewHolder(
        itemView: View,
        private val onItemClick: (PointDate) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        // 간단하게 텍스트뷰 하나만 있다고 가정 (id: tv_date for simplicity)
        // 실제로는 item_point_date.xml에 정의된 ID를 써야 함
        private val tvDate: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(item: PointDate) {
            tvDate.text = item.date
            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<PointDate>() {
            override fun areItemsTheSame(oldItem: PointDate, newItem: PointDate): Boolean {
                return oldItem.date == newItem.date
            }

            override fun areContentsTheSame(oldItem: PointDate, newItem: PointDate): Boolean {
                return oldItem == newItem
            }
        }
    }
}
