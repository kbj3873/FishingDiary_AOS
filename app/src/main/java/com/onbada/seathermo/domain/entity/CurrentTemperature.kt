package com.onbada.seathermo.domain.entity

/**
 * 현재 수온 조회 쿼리.
 *
 * [개념] data class는 equals()와 copy()를 자동 생성합니다.
 *        iOS의 struct CurrentTemperatureQuery와 동일합니다.
 *
 * @property key RISA API 인증 키.
 * @property id API 요청 ID (예: "risaList").
 * @property gruNam 해역 그룹명 (예: "서해", "동해", "남해").
 */
data class CurrentTemperatureQuery(
    val key: String,
    val id: String,
    val gruNam: String
)

/**
 * 현재 수온 정보.
 *
 * RISA API(id=risaList)로부터 받은 관측소별 수온 데이터입니다.
 *
 * @property gruNam 해역 그룹명.
 * @property staCde 관측소 코드.
 * @property obsLay 관측 위치.
 * @property staNamKor 관측소 한글명.
 * @property wtrTmp 수온 (문자열, 예: "18.5").
 */
data class CurrentTemperature(
    val gruNam: String,
    val staCde: String,
    val obsLay: String,
    val staNamKor: String,
    val wtrTmp: String
)

/**
 * 표층/중층/저층 수온을 통합한 관측소 모델 (UI 표시용).
 *
 * [개념] Kotlin의 기본값(default value)은 Swift의 기본 파라미터와 동일합니다.
 *        `var seaName: String = ""` 처럼 생성 시 값을 생략할 수 있습니다.
 *
 * @property stationCode 관측소 코드.
 * @property stationName 관측소 한글명.
 * @property surTemperature 표층 수온 문자열.
 * @property midTemperature 중층 수온 문자열.
 * @property botTemperature 저층 수온 문자열.
 * @property seaName 해역명 (기본값: 빈 문자열).
 * @property isChecked 즐겨찾기 선택 여부 (기본값: false).
 */
data class CombinedCurrentTemperature(
    val stationCode: String,
    val stationName: String,
    val surTemperature: String,
    val midTemperature: String,
    val botTemperature: String,
    val seaName: String = "",
    val isChecked: Boolean = false
)
