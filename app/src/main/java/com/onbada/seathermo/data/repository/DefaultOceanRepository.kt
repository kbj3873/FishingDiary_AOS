package com.onbada.seathermo.data.repository

import com.onbada.seathermo.data.network.APIEndpoints
import com.onbada.seathermo.data.network.datamapping.RisaInfoListRequestDTO
import com.onbada.seathermo.data.network.datamapping.RisaListRequestDTO
import com.onbada.seathermo.domain.entity.CurrentTemperature
import com.onbada.seathermo.domain.entity.CurrentTemperatureQuery
import com.onbada.seathermo.domain.entity.SeaAnalysisQuery
import com.onbada.seathermo.domain.entity.WeeklyTemperature
import com.onbada.seathermo.domain.repository.OceanRepository
import com.onbada.seathermo.infrastructure.network.NetworkError
import com.onbada.seathermo.infrastructure.network.NetworkService
import com.onbada.seathermo.infrastructure.network.request

/**
 * OceanRepository 구현체.
 *
 * iOS의 DefaultOceanRepository에 대응합니다.
 */
class DefaultOceanRepository(
    private val networkService: NetworkService
) : OceanRepository {

    /**
     * 실시간 수온 측정값 목록을 가져옵니다 (RISA Open API).
     */
    override suspend fun fetchRisaList(query: CurrentTemperatureQuery): List<CurrentTemperature> {
        val requestDTO = RisaListRequestDTO(query)
        val endpoint = APIEndpoints.getRisaJson(
            baseURL = networkService.baseURL,
            requestDTO = requestDTO
        )
        val responseDTO = networkService.request(endpoint)

        if (responseDTO.header.resultCode != "00") {
            throw NetworkError.Generic(
                Exception("해양 관측 데이터를 불러오는 데 실패했습니다. (code: ${responseDTO.header.resultCode})")
            )
        }

        return responseDTO.body.item?.map { it.toDomain() } ?: emptyList()
    }

    /**
     * 특정 측정소의 주간 수온 이력을 가져옵니다 (NIFS 웹 API).
     */
    override suspend fun fetchTemperature(query: SeaAnalysisQuery): List<WeeklyTemperature> {
        // [개념] 신규 API 스펙에 맞는 RequestDTO 변환 (iOS와 동일)
        val requestDTO = RisaInfoListRequestDTO(query)
        
        // [개념] 신규 JSON 엔드포인트 생성 (iOS와 동일)
        val endpoint = APIEndpoints.searchRisaInfoList(
            baseURL = networkService.baseURL,
            requestDTO = requestDTO
        )
        val responseDTO = networkService.request(endpoint)

        // [개념] DTO -> Domain Entity 변환 및 반환
        return responseDTO.retList.map { it.toDomain() }
    }
}
