package com.onbada.seathermo.domain.usecase

import com.onbada.seathermo.domain.entity.CurrentTemperature
import com.onbada.seathermo.domain.entity.CurrentTemperatureQuery
import com.onbada.seathermo.domain.entity.SeaAnalysisQuery
import com.onbada.seathermo.domain.entity.WeeklyTemperature
import com.onbada.seathermo.domain.repository.OceanRepository

/**
 * 해양 데이터 UseCase 인터페이스.
 *
 * [개념] UseCase는 하나의 비즈니스 흐름을 오케스트레이션하는 단위입니다.
 *        Repository를 직접 호출하지 않고 UseCase를 통해 ViewModel이 데이터를 요청합니다.
 *        iOS의 OceanUseCase protocol에 대응합니다.
 */
interface OceanUseCase {
    suspend fun fetchRisaList(query: CurrentTemperatureQuery): List<CurrentTemperature>
    suspend fun fetchTemperature(query: SeaAnalysisQuery): List<WeeklyTemperature>
}

/**
 * OceanUseCase 기본 구현체.
 *
 * [개념] Kotlin에서 final class는 기본값입니다.
 *        상속을 허용하려면 open 키워드가 필요하며,
 *        Swift의 final class와 동일하게 상속 불가를 명시할 때 사용합니다.
 *
 * [개념] private val로 선언된 생성자 파라미터는 클래스 프로퍼티로 자동 등록됩니다.
 *        Swift의 private let oceanRepository: OceanRepository와 동일합니다.
 *
 * iOS의 DefaultOceanUseCase에 대응합니다.
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
