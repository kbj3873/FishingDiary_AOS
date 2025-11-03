package com.bj.fishingdiary.data.repository

import com.bj.fishingdiary.data.network.datamapping.*
import com.bj.fishingdiary.domain.common.Cancellable
import com.bj.fishingdiary.domain.entity.*
import com.bj.fishingdiary.domain.repository.OceanRepository
import com.bj.fishingdiary.infrastructure.network.*

/**
 * OceanRepository 구현체
 * OceanRepository Implementation
 *
 * iOS의 DefaultOceanRepository.swift에 대응
 * DataTransferService를 사용하여 해양 데이터 API를 호출
 */
class DefaultOceanRepository(
    private val apiDataTransferService: DataTransferService,
    private val apiXmlTransferService: DataTransferService,
    private val backgroundQueue: DataTransferDispatchQueue = BackgroundDispatchQueue()
) : OceanRepository {

    override fun fetchRisaList(
        query: RisaListQuery,
        completion: (Result<RisaResponse<RisaList>>) -> Unit
    ): Cancellable? {
        // 1. RequestDTO 생성
        val requestDTO = RisaListRequestDTO(query)

        // 2. Endpoint 생성
        val endpoint = Endpoint<RisaListResponseDTO>(
            path = "OpenAPI_json",
            method = HTTPMethodType.POST,
            queryParameters = mapOf(
                "key" to requestDTO.key,
                "id" to requestDTO.id,
                "gru_nam" to requestDTO.gruNam
            )
        )

        // 3. Task 생성
        val task = RepositoryTask()

        // 4. API 호출
        task.networkTask = apiDataTransferService.request(
            endpoint = endpoint,
            responseType = RisaListResponseDTO::class.java,
            queue = backgroundQueue
        ) { result ->
            result.onSuccess { responseDTO ->
                // DTO를 Domain 모델로 변환
                completion(Result.success(responseDTO.toDomain()))
            }.onFailure { error ->
                completion(Result.failure(error))
            }
        }

        return task
    }

    override fun fetchStationCode(
        query: RisaCodeQuery,
        completion: (Result<RisaResponse<RisaCode>>) -> Unit
    ): Cancellable? {
        val requestDTO = RisaCodeRequestDTO(query)

        val endpoint = Endpoint<RisaCodeResponseDTO>(
            path = "OpenAPI_json",
            method = HTTPMethodType.POST,
            queryParameters = mapOf(
                "key" to requestDTO.key,
                "id" to requestDTO.id,
                "gru_nam" to requestDTO.gruNam,
                "use_yn" to requestDTO.useYn
            )
        )

        val task = RepositoryTask()

        task.networkTask = apiDataTransferService.request(
            endpoint = endpoint,
            responseType = RisaCodeResponseDTO::class.java,
            queue = backgroundQueue
        ) { result ->
            result.onSuccess { responseDTO ->
                completion(Result.success(responseDTO.toDomain()))
            }.onFailure { error ->
                completion(Result.failure(error))
            }
        }

        return task
    }

    override fun fetchRisaCoo(
        query: RisaCooQuery,
        completion: (Result<RisaResponse<RisaCoo>>) -> Unit
    ): Cancellable? {
        val requestDTO = RisaCooRequestDTO(query)

        val endpoint = Endpoint<CooListResponseDTO>(
            path = "OpenAPI_json",
            method = HTTPMethodType.POST,
            queryParameters = mapOf(
                "key" to requestDTO.key,
                "id" to requestDTO.id,
                "sdate" to requestDTO.sDate,
                "edate" to requestDTO.eDate
            )
        )

        val task = RepositoryTask()

        task.networkTask = apiDataTransferService.request(
            endpoint = endpoint,
            responseType = CooListResponseDTO::class.java,
            queue = backgroundQueue
        ) { result ->
            result.onSuccess { responseDTO ->
                completion(Result.success(responseDTO.toDomain()))
            }.onFailure { error ->
                completion(Result.failure(error))
            }
        }

        return task
    }

    override fun fetchTemperature(
        query: OceanQuery,
        completion: (Result<OceanResponse>) -> Unit
    ): Cancellable? {
        // 1. RequestDTO 생성
        val requestDTO = OceanRequestDTO(query)

        // 2. Endpoint 생성
        val endpoint = Endpoint<OceanResponseDTO>(
            path = "risa/risaInfo.risa",
            method = HTTPMethodType.POST,
            queryParameters = mapOf(
                "id" to requestDTO.id,
                "gru_nam" to requestDTO.gruNam,
                "use_yn" to requestDTO.useYn,
                "sta_cde" to requestDTO.staCde,
                "data_cnt" to requestDTO.dataCnt,
                "ord" to requestDTO.ord,
                "ord_type" to requestDTO.ordType,
                "obs_from" to requestDTO.obsFrom,
                "obs_to" to requestDTO.obsTo
            )
        )

        // 3. Task 생성
        val task = RepositoryTask()

        // 4. HTML API 호출 (HTML to JSON 파싱 필요)
        task.networkTask = apiXmlTransferService.requestHtml(
            endpoint = endpoint,
            responseType = OceanResponseDTO::class.java,
            queue = backgroundQueue
        ) { result ->
            result.onSuccess { responseDTO ->
                println("task success")
                completion(Result.success(responseDTO.toDomain()))
            }.onFailure { error ->
                completion(Result.failure(error))
            }
        }

        return task
    }
}
