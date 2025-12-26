package com.bj.fishingdiary.presentation.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bj.fishingdiary.R
import com.bj.fishingdiary.domain.entity.OceanStationModel

/**
 * 수온 관측소 선택 RecyclerView Adapter
 * Ocean Station Selection RecyclerView Adapter
 *
 * iOS의 UITableViewDataSource + OceanSelectCell에 대응
 *
 * 역할:
 * - 수온 관측소 목록 표시
 * - 체크박스를 통한 즐겨찾기 선택/해제
 * - 아이템 클릭 이벤트 처리
 *
 * @property onItemClick 아이템 클릭 리스너
 */
class OceanSelectAdapter(
    private val onItemClick: (OceanStationModel) -> Unit
) : ListAdapter<OceanStationModel, OceanSelectAdapter.OceanSelectViewHolder>(
    OceanSelectDiffCallback()
) {

    /**
     * ViewHolder 생성
     * Create ViewHolder
     *
     * @param parent RecyclerView
     * @param viewType 뷰 타입
     * @return OceanSelectViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OceanSelectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ocean_select, parent, false)
        return OceanSelectViewHolder(view, onItemClick)
    }

    /**
     * ViewHolder에 데이터 바인딩
     * Bind data to ViewHolder
     *
     * @param holder ViewHolder
     * @param position 리스트 내 위치
     */
    override fun onBindViewHolder(holder: OceanSelectViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    /**
     * ViewHolder 클래스
     * ViewHolder Class
     *
     * iOS OceanSelectCell의 역할:
     * - oceanName: 관측소 이름 라벨
     * - checkButton: 체크박스 버튼
     * - actionChecked: 체크박스 클릭 액션
     */
    class OceanSelectViewHolder(
        itemView: View,
        private val onItemClick: (OceanStationModel) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        // XML 레이아웃의 뷰들을 참조
        private val tvOceanName: TextView = itemView.findViewById(R.id.tvOceanName)
        private val ivCheckBox: ImageView = itemView.findViewById(R.id.ivCheckBox)

        private var currentModel: OceanStationModel? = null

        init {
            // 아이템 전체 클릭 리스너
            itemView.setOnClickListener {
                currentModel?.let { model ->
                    onItemClick(model)
                }
            }

            // 체크박스 클릭 리스너
            ivCheckBox.setOnClickListener {
                currentModel?.let { model ->
                    onItemClick(model)
                }
            }
        }

        /**
         * 데이터를 뷰에 바인딩
         * Bind data to view
         *
         * iOS OceanSelectCell의 fill(with viewModel:)와 동일한 역할
         *
         * @param station 해양 관측소 모델
         */
        fun bind(station: OceanStationModel) {
            currentModel = station

            // 관측소 이름 설정
            tvOceanName.text = station.stationName

            // 체크박스 상태에 따라 이미지 변경
            if (station.isChecked) {
                ivCheckBox.setImageResource(R.drawable.checkbox_selected)
            } else {
                ivCheckBox.setImageResource(R.drawable.checkbox_normal)
            }
        }
    }

    /**
     * DiffUtil.ItemCallback 구현
     * DiffUtil.ItemCallback Implementation
     */
    class OceanSelectDiffCallback : DiffUtil.ItemCallback<OceanStationModel>() {

        /**
         * 두 아이템이 동일한 항목인지 확인
         * Check if two items are the same
         */
        override fun areItemsTheSame(
            oldItem: OceanStationModel,
            newItem: OceanStationModel
        ): Boolean {
            return oldItem.stationCode == newItem.stationCode
        }

        /**
         * 두 아이템의 내용이 동일한지 확인
         * Check if contents of two items are the same
         */
        override fun areContentsTheSame(
            oldItem: OceanStationModel,
            newItem: OceanStationModel
        ): Boolean {
            return oldItem == newItem
        }
    }
}
