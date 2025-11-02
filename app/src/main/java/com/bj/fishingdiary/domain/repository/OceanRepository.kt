package com.bj.fishingdiary.domain.repository

import com.bj.fishingdiary.domain.common.Cancellable
import com.bj.fishingdiary.domain.entity.*

/**
 * 해양 데이터 Repository 인터페이스
 * Ocean Data Repository Interface
 *
 * Repository 패턴이란?
 * - 데이터 소스(API, 데이터베이스 등)에 접근하는 로직을 추상화
 * - Domain 레이어는 인터페이스만 알고, 실제 구현은 Data 레이어에 위치
 * - 데이터 소스가 변경되어도 Domain 레이어에 영향 없음 (의존성 역전 원칙)
 *
 * What is Repository Pattern?
 * - Abstracts logic for accessing data sources (API, database, etc.)
 * - Domain layer only knows the interface, actual implementation is in Data layer
 * - No impact on Domain layer even if data source changes (Dependency Inversion Principle)
 *
 * Clean Architecture의 핵심 개념:
 * - Domain 레이어: Repository 인터페이스 정의 (이 파일)
 * - Data 레이어: Repository 구현체 (DefaultOceanRepository)
 * - Domain은 Data를 알지 못하고, Data가 Domain을 참조
 *
 * Core concept of Clean Architecture:
 * - Domain layer: Defines Repository interface (this file)
 * - Data layer: Repository implementation (DefaultOceanRepository)
 * - Domain doesn't know Data, Data references Domain
 *
 * iOS의 OceanRepository protocol과 동일한 역할
 * Same role as iOS's OceanRepository protocol
 */
interface OceanRepository {

    /**
     * RISA 수온 목록 조회
     * Fetch RISA temperature list
     *
     * @param query 조회 쿼리 파라미터
     * @param completion 완료 콜백 함수
     *                   - Result<RisaResponse<RisaList>, Throwable>: 성공 시 데이터, 실패 시 에러
     *                   - Kotlin의 Result는 Swift의 Result<T, Error>와 동일한 개념
     * @return 취소 가능한 작업 객체 (nullable)
     *
     * 콜백(Callback)이란?
     * - 비동기 작업이 완료되었을 때 호출되는 함수
     * - iOS의 completion handler와 동일한 개념
     * - 예: completion(Result.success(data)) 또는 completion(Result.failure(error))
     */
    fun fetchRisaList(
        query: RisaListQuery,
        completion: (Result<RisaResponse<RisaList>>) -> Unit
    ): Cancellable?

    /**
     * RISA 관측소 코드 조회
     * Fetch RISA station code
     *
     * @param query 조회 쿼리 파라미터
     * @param completion 완료 콜백
     * @return 취소 가능한 작업 객체
     */
    fun fetchStationCode(
        query: RisaCodeQuery,
        completion: (Result<RisaResponse<RisaCode>>) -> Unit
    ): Cancellable?

    /**
     * RISA COO 수온 데이터 조회
     * Fetch RISA COO temperature data
     *
     * @param query 조회 쿼리 파라미터
     * @param completion 완료 콜백
     * @return 취소 가능한 작업 객체
     */
    fun fetchRisaCoo(
        query: RisaCooQuery,
        completion: (Result<RisaResponse<RisaCoo>>) -> Unit
    ): Cancellable?

    /**
     * 해양 수온 상세 정보 조회
     * Fetch ocean temperature detail information
     *
     * @param query 조회 쿼리 파라미터
     * @param completion 완료 콜백
     * @return 취소 가능한 작업 객체
     */
    fun fetchTemperature(
        query: OceanQuery,
        completion: (Result<OceanResponse>) -> Unit
    ): Cancellable?
}

/**
 * Kotlin Result 타입 사용 방법:
 * How to use Kotlin Result type:
 *
 * 성공 케이스:
 * Success case:
 * ```kotlin
 * completion(Result.success(oceanResponse))
 * ```
 *
 * 실패 케이스:
 * Failure case:
 * ```kotlin
 * completion(Result.failure(Exception("Error message")))
 * ```
 *
 * 결과 처리:
 * Handle result:
 * ```kotlin
 * repository.fetchTemperature(query) { result ->
 *     result.onSuccess { data ->
 *         // 성공 시 처리
 *         // Handle success
 *         println("Data: $data")
 *     }
 *     result.onFailure { error ->
 *         // 실패 시 처리
 *         // Handle failure
 *         println("Error: ${error.message}")
 *     }
 * }
 * ```
 */
