package com.bj.fishingdiary.domain.usecase

import com.bj.fishingdiary.domain.common.Cancellable
import com.bj.fishingdiary.domain.entity.*
import com.bj.fishingdiary.domain.repository.OceanRepository

/**
 * 해양 데이터 UseCase
 * Ocean Data UseCase
 *
 * UseCase의 역할:
 * Role of UseCase:
 * 1. 비즈니스 로직 캡슐화 (Encapsulate business logic)
 * 2. Repository를 호출하여 데이터 가져오기 (Call Repository to fetch data)
 * 3. 필요시 데이터 가공 및 변환 (Process and transform data if needed)
 * 4. Presentation 레이어에 결과 전달 (Deliver result to Presentation layer)
 *
 * Clean Architecture 흐름:
 * Clean Architecture flow:
 * ViewModel -> UseCase -> Repository -> API/DB
 *
 * @property oceanRepository 해양 데이터 Repository (의존성 주입)
 *                          Dependency Injection을 통해 외부에서 주입받음
 */
class OceanUseCase(
    private val oceanRepository: OceanRepository
) {

    /**
     * RISA 수온 목록 조회 실행
     * Execute RISA temperature list fetch
     *
     * @param requestValue 요청 값 객체 (쿼리 파라미터를 캡슐화)
     * @param completion 완료 콜백
     * @return 취소 가능한 작업 객체
     */
    fun executeRisaList(
        requestValue: RisaListRequestValue,
        completion: (Result<RisaResponse<RisaList>>) -> Unit
    ): Cancellable? {
        // Repository에 작업 위임
        // Delegate work to Repository
        return oceanRepository.fetchRisaList(
            query = requestValue.query,
            completion = { result ->
                // 비즈니스 로직 추가 가능 (예: 데이터 필터링, 정렬 등)
                // Can add business logic here (e.g., data filtering, sorting, etc.)
                completion(result)
            }
        )
    }

    /**
     * RISA 관측소 코드 조회 실행
     * Execute RISA station code fetch
     */
    fun executeStationCode(
        requestValue: RisaCodeRequestValue,
        completion: (Result<RisaResponse<RisaCode>>) -> Unit
    ): Cancellable? {
        return oceanRepository.fetchStationCode(
            query = requestValue.query,
            completion = { result ->
                completion(result)
            }
        )
    }

    /**
     * RISA COO 수온 데이터 조회 실행
     * Execute RISA COO temperature data fetch
     */
    fun executeRisaCoo(
        requestValue: RisaCooRequestValue,
        completion: (Result<RisaResponse<RisaCoo>>) -> Unit
    ): Cancellable? {
        return oceanRepository.fetchRisaCoo(
            query = requestValue.query,
            completion = { result ->
                completion(result)
            }
        )
    }

    /**
     * 해양 수온 상세 정보 조회 실행
     * Execute ocean temperature detail fetch
     */
    fun execute(
        requestValue: OceanRequestValue,
        completion: (Result<OceanResponse>) -> Unit
    ): Cancellable? {
        return oceanRepository.fetchTemperature(
            query = requestValue.query,
            completion = { result ->
                completion(result)
            }
        )
    }
}

/**
 * Request Value 객체들
 * Request Value Objects
 *
 * 요청 파라미터를 캡슐화하는 래퍼 클래스
 * Wrapper classes that encapsulate request parameters
 *
 * 왜 필요한가?
 * Why needed?
 * - UseCase의 메서드 시그니처를 단순하게 유지
 * - 요청 파라미터가 추가되어도 메서드 시그니처 변경 불필요
 * - 요청 데이터를 하나의 객체로 그룹화하여 가독성 향상
 *
 * - Keep UseCase method signatures simple
 * - No need to change method signature even if request parameters are added
 * - Improve readability by grouping request data into one object
 */

/**
 * RISA 수온 목록 요청 값
 * RISA temperature list request value
 */
data class RisaListRequestValue(
    val query: RisaListQuery
)

/**
 * RISA 관측소 코드 요청 값
 * RISA station code request value
 */
data class RisaCodeRequestValue(
    val query: RisaCodeQuery
)

/**
 * RISA COO 수온 데이터 요청 값
 * RISA COO temperature data request value
 */
data class RisaCooRequestValue(
    val query: RisaCooQuery
)

/**
 * 해양 수온 상세 요청 값
 * Ocean temperature detail request value
 */
data class OceanRequestValue(
    val query: OceanQuery
)

/**
 * UseCase 사용 예시:
 * UseCase usage example:
 *
 * ```kotlin
 * // 1. UseCase 인스턴스 생성 (DI 컨테이너에서 주입받음)
 * // Create UseCase instance (injected from DI container)
 * val oceanUseCase = OceanUseCase(oceanRepository)
 *
 * // 2. Request Value 생성
 * // Create Request Value
 * val query = RisaListQuery(
 *     key = "API_KEY",
 *     id = "risaList",
 *     gruNam = "서해"
 * )
 * val requestValue = RisaListRequestValue(query)
 *
 * // 3. UseCase 실행
 * // Execute UseCase
 * val cancellable = oceanUseCase.executeRisaList(requestValue) { result ->
 *     result.onSuccess { response ->
 *         // 성공: UI 업데이트
 *         // Success: Update UI
 *         println("Data: ${response.body?.item}")
 *     }
 *     result.onFailure { error ->
 *         // 실패: 에러 처리
 *         // Failure: Handle error
 *         println("Error: ${error.message}")
 *     }
 * }
 *
 * // 4. 필요 시 취소
 * // Cancel if needed
 * cancellable?.cancel()
 * ```
 */
