package com.bj.fishingdiary.domain.entity

/**
 * 해수 온도 관련 Entity
 * Sea Water Temperature Related Entities
 *
 * 해수 온도 데이터를 조회하고 표시하기 위한 데이터 모델
 * Data models for querying and displaying sea water temperature data
 */

/**
 * 해수 온도 요청 모델
 * Sea Water Temperature Request Model
 *
 * API 호출 시 필요한 파라미터를 담는 요청 객체
 * Request object containing parameters needed for API call
 *
 * @property id API 요청 ID
 * @property gruNam 해역 그룹명 (예: "서해", "동해", "남해")
 * @property useYn 사용 여부 ("Y" 또는 "N")
 * @property staCde 관측소 코드 (Station Code)
 * @property dataCnt 조회할 데이터 개수 (Data Count)
 * @property ord 정렬 기준 컬럼 (Order by column)
 * @property ordType 정렬 타입 ("ASC" 오름차순, "DESC" 내림차순)
 * @property obsFrom 관측 시작 날짜 (Observation From)
 * @property obsTo 관측 종료 날짜 (Observation To)
 */
data class SeaWaterTemperatureRequestModel(
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

/**
 * 해수 온도 세트 (3개 층의 온도 정보)
 * Sea Water Temperature Set (temperature info for 3 layers)
 *
 * 특정 시점의 표층/중층/저층 온도와 깊이 정보
 * Surface/Middle/Bottom temperature and depth information at a specific time
 *
 * @property surTemp 표층 온도 (Surface Temperature) - 섭씨(Celsius)
 * @property midTemp 중층 온도 (Middle Temperature)
 * @property botTemp 저층 온도 (Bottom Temperature)
 * @property surDep 표층 깊이 (Surface Depth) - 미터(meter)
 * @property midDep 중층 깊이 (Middle Depth)
 * @property botDep 저층 깊이 (Bottom Depth)
 * @property dateTime 측정 일시 (타임스탬프)
 */
data class SeaWaterTemperatureSet(
    val surTemp: Float,        // Float: 소수점을 포함하는 숫자 타입
    val midTemp: Float,
    val botTemp: Float,
    val surDep: Float,
    val midDep: Float,
    val botDep: Float,
    val dateTime: Int          // Int: 정수 타입, Unix 타임스탬프
)
