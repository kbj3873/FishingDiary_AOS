package com.bj.fishingdiary.presentation.main

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bj.fishingdiary.R
import com.bj.fishingdiary.domain.entity.OceanStationModel

/**
 * 수온 정보 RecyclerView Adapter
 * Temperature Information RecyclerView Adapter
 *
 * iOS의 UITableViewDataSource + TempuratureCell에 대응
 *
 * RecyclerView란?
 * - Android에서 스크롤 가능한 리스트를 표시하는 뷰
 * - iOS의 UITableView와 유사하지만 더 유연하고 성능이 좋음
 * - ViewHolder 패턴을 강제하여 뷰 재사용 최적화
 *
 * ListAdapter란?
 * - RecyclerView.Adapter의 개선된 버전
 * - DiffUtil을 사용하여 데이터 변경 시 효율적으로 UI 업데이트
 * - submitList()로 새 데이터를 전달하면 자동으로 변경사항 계산 및 애니메이션 적용
 *
 * @constructor RecyclerView Adapter 생성
 */
class TemperatureAdapter : ListAdapter<OceanStationModel, TemperatureAdapter.TemperatureViewHolder>(
    TemperatureDiffCallback()
) {

    /**
     * ViewHolder 생성
     * Create ViewHolder
     *
     * RecyclerView가 새로운 ViewHolder가 필요할 때 호출
     * - 레이아웃 파일을 inflate하여 ViewHolder 생성
     * - iOS의 dequeueReusableCell과 유사
     *
     * @param parent RecyclerView
     * @param viewType 뷰 타입 (여러 종류의 아이템이 있을 때 구분용)
     * @return TemperatureViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemperatureViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_temperature, parent, false)
        return TemperatureViewHolder(view)
    }

    /**
     * ViewHolder에 데이터 바인딩
     * Bind data to ViewHolder
     *
     * RecyclerView가 ViewHolder를 화면에 표시할 때 호출
     * - position에 해당하는 데이터를 ViewHolder에 바인딩
     * - iOS의 cellForRowAt과 유사
     *
     * @param holder ViewHolder
     * @param position 리스트 내 위치
     */
    override fun onBindViewHolder(holder: TemperatureViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    /**
     * ViewHolder 클래스
     * ViewHolder Class
     *
     * ViewHolder란?
     * - 각 아이템 뷰를 담는 컨테이너
     * - findViewById를 반복적으로 호출하지 않고 뷰를 재사용하여 성능 향상
     * - iOS의 UITableViewCell과 유사한 역할
     *
     * iOS TempuratureCell의 역할:
     * - @IBOutlet var oceanName: UILabel!
     * - @IBOutlet var surTempurature: UILabel!
     * - @IBOutlet var midTempurature: UILabel!
     * - @IBOutlet var botTempurature: UILabel!
     */
    class TemperatureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        // XML 레이아웃의 뷰들을 참조
        // Reference views from XML layout
        private val tvOceanName: TextView = itemView.findViewById(R.id.tvOceanName)
        private val tvSurTemperature: TextView = itemView.findViewById(R.id.tvSurTemperature)
        private val tvMidTemperature: TextView = itemView.findViewById(R.id.tvMidTemperature)
        private val tvBotTemperature: TextView = itemView.findViewById(R.id.tvBotTemperature)

        /**
         * 데이터를 뷰에 바인딩
         * Bind data to view
         *
         * iOS TempuratureCell의 fill(with stationModel:)와 동일한 역할
         *
         * @param station 해양 관측소 모델
         */
        fun bind(station: OceanStationModel) {
            tvOceanName.text = station.stationName
            tvSurTemperature.text = station.surTemperature
            tvMidTemperature.text = station.midTemperature
            tvBotTemperature.text = station.botTemperature
            Log.d("D", station.stationName + " " + station.surTemperature + " " + station.midTemperature + " "  + station.botTemperature)
        }
    }

    /**
     * DiffUtil.ItemCallback 구현
     * DiffUtil.ItemCallback Implementation
     *
     * DiffUtil이란?
     * - 리스트의 변경사항을 효율적으로 계산하는 유틸리티
     * - 기존 리스트와 새 리스트를 비교하여 최소한의 업데이트만 수행
     * - 자동으로 애니메이션 적용
     *
     * 왜 필요한가?
     * - 전체 리스트를 다시 그리지 않고 변경된 항목만 업데이트하여 성능 향상
     * - iOS의 DiffableDataSource와 유사한 개념
     */
    class TemperatureDiffCallback : DiffUtil.ItemCallback<OceanStationModel>() {

        /**
         * 두 아이템이 동일한 항목인지 확인
         * Check if two items are the same
         *
         * 보통 ID나 고유 키를 비교
         * - 같은 아이템이지만 내용이 달라진 경우 구분
         *
         * @param oldItem 이전 아이템
         * @param newItem 새 아이템
         * @return 동일한 항목이면 true
         */
        override fun areItemsTheSame(
            oldItem: OceanStationModel,
            newItem: OceanStationModel
        ): Boolean {
            // stationCode가 고유 키 역할
            return oldItem.stationCode == newItem.stationCode
        }

        /**
         * 두 아이템의 내용이 동일한지 확인
         * Check if contents of two items are the same
         *
         * 실제 데이터 내용을 비교
         * - 내용이 같으면 UI 업데이트 불필요
         *
         * @param oldItem 이전 아이템
         * @param newItem 새 아이템
         * @return 내용이 동일하면 true
         */
        override fun areContentsTheSame(
            oldItem: OceanStationModel,
            newItem: OceanStationModel
        ): Boolean {
            // data class의 equals()를 사용하여 모든 필드 비교
            return oldItem == newItem
        }
    }
}

/**
 * Adapter 사용 예시:
 * Adapter Usage Example:
 *
 * ```kotlin
 * // 1. Adapter 생성
 * val adapter = TemperatureAdapter()
 *
 * // 2. RecyclerView에 연결
 * recyclerView.adapter = adapter
 * recyclerView.layoutManager = LinearLayoutManager(context)
 *
 * // 3. 데이터 업데이트
 * adapter.submitList(oceanStationList)
 * ```
 */
