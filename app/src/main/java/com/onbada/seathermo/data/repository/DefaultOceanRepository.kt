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
 *
 * [개념] 생성자 파라미터 private val은 클래스 프로퍼티로 자동 등록됩니다.
 *        Swift의 private let apiNetworkService: NetworkService와 동일합니다.
 */
class DefaultOceanRepository(
    private val networkService: NetworkService
) : OceanRepository {

    /**
     * 실시간 수온 측정값 목록을 가져옵니다 (RISA Open API).
     *
     * [개념] suspend override fun은 인터페이스의 suspend fun을 구현합니다.
     *        iOS의 func fetchRisaList(...) async throws에 대응합니다.
     */
    override suspend fun fetchRisaList(query: CurrentTemperatureQuery): List<CurrentTemperature> {
        val requestDTO = RisaListRequestDTO(query)
        val endpoint = APIEndpoints.getRisaJson(
            baseURL = networkService.baseURL,
            requestDTO = requestDTO
        )
        val responseDTO = networkService.request(endpoint)

        // resultCode가 "00"이 아니면 API 오류로 처리합니다.
        // [개념] throw는 Swift의 throw와 동일합니다.
        //        suspend fun에서 throw하면 호출 측의 try-catch 또는 ViewModel의 catch 블록이 받습니다.
        if (responseDTO.header.resultCode != "00") {
            throw NetworkError.Generic(
                Exception("해양 관측 데이터를 불러오는 데 실패했습니다. (code: ${responseDTO.header.resultCode})")
            )
        }

        // [개념] ?.map { } 는 null-safe map입니다. item이 null이면 null을 반환합니다.
        //        ?: emptyList() 로 null일 때 빈 리스트를 반환합니다.
        //        iOS의 responseDTO.body.item?.map { $0.toDomain() } ?? []에 대응합니다.
        return responseDTO.body.item?.map { it.toDomain() } ?: emptyList()
    }

    /**
     * 특정 측정소의 주간 수온 이력을 가져옵니다 (NIFS 웹 API).
     */
    override suspend fun fetchTemperature(query: SeaAnalysisQuery): List<WeeklyTemperature> {
        val requestDTO = RisaInfoListRequestDTO(query)
        val endpoint = APIEndpoints.searchRisaInfoList(
            baseURL = networkService.baseURL,
            requestDTO = requestDTO
        )
        val responseDTO = networkService.request(endpoint)

        return responseDTO.retList.map { it.toDomain() }
    }
}
