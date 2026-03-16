package com.onbada.seathermo.domain.repository

import com.onbada.seathermo.domain.entity.CurrentTemperature
import com.onbada.seathermo.domain.entity.CurrentTemperatureQuery
import com.onbada.seathermo.domain.entity.SeaAnalysisQuery
import com.onbada.seathermo.domain.entity.WeeklyTemperature

/**
 * 해양 데이터 Repository 인터페이스.
 */
interface OceanRepository {

    /**
     * 실시간 수온 측정값 목록을 가져옵니다 (RISA API).
     */
    suspend fun fetchRisaList(query: CurrentTemperatureQuery): List<CurrentTemperature>

    /**
     * 특정 측정소의 주간 수온 이력을 가져옵니다 (수온 분석용).
     */
    suspend fun fetchTemperature(query: SeaAnalysisQuery): List<WeeklyTemperature>
}
