package com.bj.fishingdiary.presentation.point.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bj.fishingdiary.R
import com.bj.fishingdiary.domain.entity.PointData

class PointDataAdapter(
    private val onItemClick: (PointData) -> Unit
) : ListAdapter<PointData, PointDataAdapter.PointDataViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PointDataViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_point_date, parent, false)
        return PointDataViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: PointDataViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PointDataViewHolder(
        itemView: View,
        private val onItemClick: (PointData) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val tvTitle: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(item: PointData) {
            tvTitle.text = "${item.dataName} 회차" // e.g. "0 회차"
            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<PointData>() {
            override fun areItemsTheSame(oldItem: PointData, newItem: PointData): Boolean {
                return oldItem.dataName == newItem.dataName && oldItem.dataPath == newItem.dataPath
            }

            override fun areContentsTheSame(oldItem: PointData, newItem: PointData): Boolean {
                return oldItem == newItem
            }
        }
    }
}
