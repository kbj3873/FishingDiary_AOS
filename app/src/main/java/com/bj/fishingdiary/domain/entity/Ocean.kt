package com.bj.fishingdiary.domain.entity

/**
 * 해양 데이터 관련 Entity 모음
 * Collection of Ocean-related Entities
 *
 * Entity란?
 * - 비즈니스 로직의 핵심 데이터 모델
 * - 순수한 데이터 구조로, UI나 데이터베이스에 의존하지 않음
 * - iOS의 struct와 유사하며, Kotlin에서는 data class로 구현
 *
 * What is an Entity?
 * - Core data model of business logic
 * - Pure data structure, independent of UI or database
 * - Similar to iOS struct, implemented as Kotlin data class
 */

/**
 * API 응답 헤더
 * API Response Header
 *
 * data class란?
 * - 데이터를 담기 위한 클래스
 * - 자동으로 equals(), hashCode(), toString(), copy() 메서드 생성
 * - iOS의 struct Equatable과 유사
 *
 * @property resultCode 결과 코드 (예: "200", "500")
 * @property resultMsg 결과 메시지 (예: "성공", "실패")
 */
data class Header(
    val resultCode: String,
    val resultMsg: String
)

/**
 * RISA API 응답 구조
 * RISA API Response Structure
 *
 * 제네릭 타입 <T>:
 * - 다양한 타입의 데이터를 담을 수 있는 범용 클래스
 * - iOS의 제네릭과 동일한 개념
 * - 예: RisaResponse<RisaList>, RisaResponse<RisaCode>
 *
 * @property header API 응답 헤더
 * @property body API 응답 바디 (데이터가 없을 수 있으므로 nullable)
 */
data class RisaResponse<T>(
    val header: Header,
    val body: RisaBody<T>?
)

/**
 * RISA API 응답 바디
 * RISA API Response Body
 *
 * @property item 응답 데이터 리스트
 */
data class RisaBody<T>(
    val item: List<T>
)

/**
 * RISA 수온 목록 응답 데이터 (id=risaList)
 * RISA Temperature List Response Data
 *
 * @property gruNam 해역 그룹명 (예: "서해", "동해", "남해")
 * @property staCde 관측소 코드
 * @property obsLay 관측 위치
 * @property staNamKor 관측소 한글 이름
 * @property wtrTmp 수온 (Water Temperature)
 */
data class RisaList(
    val gruNam: String,
    val staCde: String,
    val obsLay: String,
    val staNamKor: String,
    val wtrTmp: String
)

/**
 * RISA 관측소 코드 응답 데이터 (id=risaCode)
 * RISA Station Code Response Data
 *
 * @property gruNam 해역 그룹명
 * @property staCde 관측소 코드
 * @property staNamKor 관측소 한글 이름
 */
data class RisaCode(
    val gruNam: String,
    val staCde: String,
    val staNamKor: String
)

/**
 * RISA COO 수온 데이터 (id=cooList)
 * RISA COO Temperature Data
 *
 * @property gruNam 해역 그룹명
 * @property staCde 관측소 코드
 * @property staNamKor 관측소 한글 이름
 * @property obsDate 관측 날짜
 * @property wtrTmp 수온
 */
data class RisaCoo(
    val gruNam: String,
    val staCde: String,
    val staNamKor: String,
    val obsDate: String,
    val wtrTmp: String
)

/**
 * 해양 수온 응답 데이터
 * Ocean Temperature Response Data
 *
 * @property list 해양 데이터 목록
 */
data class OceanResponse(
    val list: List<Ocean>
)

/**
 * 해양 수온 상세 정보
 * Ocean Temperature Detail Information
 *
 * 수온 데이터는 3개 층으로 구분:
 * Temperature data is divided into 3 layers:
 * - 표층 (Surface): wtrTempS, surDep
 * - 중층 (Middle): wtrTempM, midDep
 * - 저층 (Bottom): wtrTempB, botDep
 *
 * @property staCde 관측소 코드
 * @property staNamKor 관측소 한글 이름
 * @property staNam 관측소 영문 이름
 * @property obsDtm 관측 일시 (Observation Date Time)
 * @property wtrTempS 표층 수온 (Surface Water Temperature)
 * @property surDep 표층 깊이 (Surface Depth)
 * @property wtrTempM 중층 수온 (Middle Water Temperature)
 * @property midDep 중층 깊이 (Middle Depth)
 * @property wtrTempB 저층 수온 (Bottom Water Temperature)
 * @property botDep 저층 깊이 (Bottom Depth)
 * @property lon 경도 (Longitude)
 * @property lat 위도 (Latitude)
 * @property dateT 날짜 타임스탬프
 */
data class Ocean(
    val staCde: String,
    val staNamKor: String,
    val staNam: String,
    val obsDtm: String,
    val wtrTempS: Float,
    val surDep: Float,
    val wtrTempM: Float,
    val midDep: Float,
    val wtrTempB: Float,
    val botDep: Float,
    val lon: Float,
    val lat: Float,
    val dateT: Int
)

/**
 * 해양 관측소 모델 (UI 표시용)
 * Ocean Station Model (for UI display)
 *
 * @property stationCode 관측소 코드
 * @property stationName 관측소 이름
 * @property surTemperature 표층 온도 (문자열 형식, UI 표시용)
 * @property midTemperature 중층 온도
 * @property botTemperature 저층 온도
 * @property isChecked 사용자가 선택(즐겨찾기)했는지 여부
 */
data class OceanStationModel(
    val stationCode: String,
    val stationName: String,
    val surTemperature: String,
    val midTemperature: String,
    val botTemperature: String,
    val isChecked: Boolean = false  // 기본값은 선택되지 않음
)
