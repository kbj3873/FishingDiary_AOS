package com.onbada.seathermo.domain.usecase

import com.onbada.seathermo.domain.entity.CurrentTemperature
import com.onbada.seathermo.domain.entity.CurrentTemperatureQuery
import com.onbada.seathermo.domain.entity.SeaAnalysisQuery
import com.onbada.seathermo.domain.entity.WeeklyTemperature
import com.onbada.seathermo.domain.repository.OceanRepository

/**
 * 해양 데이터 UseCase 인터페이스.
 */
interface OceanUseCase {
    suspend fun fetchRisaList(query: CurrentTemperatureQuery): List<CurrentTemperature>
    suspend fun fetchTemperature(query: SeaAnalysisQuery): List<WeeklyTemperature>
}

/**
 * OceanUseCase 기본 구현체.
 */
class DefaultOceanUseCase(
    private val oceanRepository: OceanRepository
) : OceanUseCase {

    override suspend fun fetchRisaList(query: CurrentTemperatureQuery): List<CurrentTemperature> {
        return oceanRepository.fetchRisaList(query)
    }

    override suspend fun fetchTemperature(query: SeaAnalysisQuery): List<WeeklyTemperature> {
        return oceanRepository.fetchTemperature(query)
    }
}
