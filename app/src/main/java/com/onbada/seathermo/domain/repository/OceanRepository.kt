package com.onbada.seathermo.domain.repository

import com.onbada.seathermo.domain.entity.CurrentTemperature
import com.onbada.seathermo.domain.entity.CurrentTemperatureQuery
import com.onbada.seathermo.domain.entity.SeaAnalysisQuery
import com.onbada.seathermo.domain.entity.WeeklyTemperature

/**
 * 해양 데이터 Repository 인터페이스.
 *
 * [개념] interface는 Swift의 protocol과 동일합니다.
 *        구현 코드 없이 함수 시그니처만 선언하며,
 *        실제 구현은 data 레이어의 DefaultOceanRepository가 담당합니다.
 *
 * [개념] suspend fun은 코루틴 안에서만 호출할 수 있는 '일시 중단 가능한 함수'입니다.
 *        네트워크 I/O 등 시간이 걸리는 작업을 스레드 블로킹 없이 처리합니다.
 *        Swift의 async func와 동일한 개념입니다.
 *        예외 발생 시 자동으로 throw되므로 Swift의 throws 키워드가 별도로 필요 없습니다.
 *
 * iOS의 OceanRepository protocol에 대응합니다.
 */
interface OceanRepository {

    /**
     * 실시간 수온 측정값 목록을 가져옵니다 (RISA API).
     *
     * @param query 조회 조건 (API 키, 측정소 ID, 해역명 등).
     * @return 측정소별 현재 수온 목록.
     */
    suspend fun fetchRisaList(query: CurrentTemperatureQuery): List<CurrentTemperature>

    /**
     * 특정 측정소의 주간 수온 이력을 가져옵니다 (수온 분석용).
     *
     * @param query 조회 조건 (측정소 코드, 조회 기간 등).
     * @return 날짜별 수온 기록 목록.
     */
    suspend fun fetchTemperature(query: SeaAnalysisQuery): List<WeeklyTemperature>
}
