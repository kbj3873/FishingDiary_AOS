package com.onbada.seathermo.domain.entity

/**
 * 수온 분석(SeaAnalysis) 화면을 위한 API 조회 쿼리.
 *
 * iOS의 SeaAnalysisQuery struct와 동일합니다.
 *
 * @property id API 요청 ID.
 * @property gruNam 해역 그룹명.
 * @property useYn 사용 여부 ("Y" 또는 "N").
 * @property staCde 관측소 코드.
 * @property dataCnt 조회할 데이터 개수.
 * @property ord 정렬 기준 컬럼.
 * @property ordType 정렬 방식 ("ASC" 또는 "DESC").
 * @property obsFrom 관측 시작 날짜.
 * @property obsTo 관측 종료 날짜.
 */
data class SeaAnalysisQuery(
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
 * 주간 수온 데이터 (수온 분석 화면용).
 *
 * iOS의 WeeklyTemperature struct와 동일합니다.
 *
 * [개념] Kotlin의 val vs var:
 *        val은 한 번 할당하면 변경 불가 (Swift의 let).
 *        var는 재할당 가능 (Swift의 var).
 *        iOS에서 dateT만 var로 선언된 것에 맞춰 Kotlin도 var로 선언합니다.
 *
 * @property staCde 관측소 코드.
 * @property staNamKor 관측소 한글명.
 * @property staNam 관측소 영문명.
 * @property obsDtm 관측 일시 문자열.
 * @property wtrTempS 표층 수온.
 * @property surDep 표층 깊이.
 * @property wtrTempM 중층 수온.
 * @property midDep 중층 깊이.
 * @property wtrTempB 저층 수온.
 * @property botDep 저층 깊이.
 * @property lon 경도.
 * @property lat 위도.
 * @property dateT 날짜 타임스탬프.
 *                 [개념] iOS는 Int, Android는 Long을 사용합니다.
 *                        Long은 64비트 정수로 타임스탬프 처리에 적합합니다.
 */
data class WeeklyTemperature(
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
    var dateT: Long
)
