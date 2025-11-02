package com.bj.fishingdiary.domain.entity

/**
 * 해양 API 요청 쿼리 모델
 * Ocean API Request Query Models
 *
 * Query란?
 * - API 요청 시 필요한 파라미터를 담는 데이터 모델
 * - Repository로 전달되어 실제 API 호출에 사용됨
 * - iOS의 Request Value 객체와 동일한 역할
 */

/**
 * RISA 쿼리 기본 인터페이스
 * Base RISA Query Interface
 *
 * interface란?
 * - 여러 클래스가 공통으로 구현해야 할 규약을 정의
 * - iOS의 protocol과 동일한 개념
 * - 다형성(Polymorphism)을 제공하여 다양한 쿼리 타입을 동일하게 처리 가능
 *
 * @property key API 인증 키
 * @property id API 요청 ID (어떤 데이터를 요청하는지 구분)
 */
interface RisaQuery {
    val key: String
    val id: String
}

/**
 * RISA 수온 목록 조회 쿼리 (id=risaList)
 * RISA Temperature List Query
 *
 * @property key API 인증 키
 * @property id API 요청 ID ("risaList")
 * @property gruNam 해역 그룹명 (예: "서해", "동해", "남해")
 */
data class RisaListQuery(
    override val key: String,
    override val id: String,
    val gruNam: String
) : RisaQuery  // RisaQuery 인터페이스를 구현

/**
 * RISA 관측소 코드 조회 쿼리 (id=risaCode)
 * RISA Station Code Query
 *
 * @property key API 인증 키
 * @property id API 요청 ID ("risaCode")
 * @property gruNam 해역 그룹명
 * @property useYn 사용 여부 ("Y" 또는 "N")
 */
data class RisaCodeQuery(
    override val key: String,
    override val id: String,
    val gruNam: String,
    val useYn: String
) : RisaQuery

/**
 * RISA COO 수온 데이터 조회 쿼리 (id=cooList)
 * RISA COO Temperature Data Query
 *
 * @property key API 인증 키
 * @property id API 요청 ID ("cooList")
 * @property sDate 시작 날짜 (Start Date)
 * @property eDate 종료 날짜 (End Date)
 */
data class RisaCooQuery(
    override val key: String,
    override val id: String,
    val sDate: String,
    val eDate: String
) : RisaQuery

/**
 * 해양 수온 상세 조회 쿼리
 * Ocean Temperature Detail Query
 *
 * 더 복잡한 조회 조건을 지원하는 쿼리:
 * More complex query supporting advanced search conditions:
 * - 데이터 개수 제한 (dataCnt)
 * - 정렬 조건 (ord, ordType)
 * - 날짜 범위 (obsFrom, obsTo)
 *
 * @property id API 요청 ID
 * @property gruNam 해역 그룹명
 * @property useYn 사용 여부
 * @property staCde 관측소 코드 (특정 관측소만 조회 시 사용)
 * @property dataCnt 조회할 데이터 개수
 * @property ord 정렬 기준 컬럼명 (Order)
 * @property ordType 정렬 방식 ("ASC" 오름차순, "DESC" 내림차순)
 * @property obsFrom 관측 시작 날짜 (Observation From)
 * @property obsTo 관측 종료 날짜 (Observation To)
 */
data class OceanQuery(
    val id: String,
    val gruNam: String,
    val useYn: String,
    val staCde: String,
    val dataCnt: String,
    val ord: String,
    val ordType: String,
    val obsFrom: String,
    val obsTo: String
)
