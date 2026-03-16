package com.onbada.seathermo.domain.entity

/**
 * 한국 해역 구분.
 *
 * [개념] Kotlin enum class는 Swift enum과 유사하지만,
 *        각 케이스가 생성자 파라미터를 가질 수 있습니다.
 *        Swift의 `var id: String { switch self { ... } }` 계산 프로퍼티 대신
 *        Kotlin은 생성자에서 값을 직접 받아 저장합니다.
 *
 * 제주는 남해(S)로 간주합니다.
 *
 * @property displayName 화면에 표시될 한글명.
 * @property id NIFS API gruNam 코드.
 */
enum class Sea(val displayName: String, val id: String) {
    NONE("선택", ""),
    WEST("서해", "W"),
    EAST("동해", "E"),
    SOUTH("남해", "S");

    companion object {
        // [개념] companion object는 Swift의 static 메서드와 동일합니다.
        //        클래스 인스턴스 없이 Sea.fromId("W") 형태로 호출합니다.
        fun fromDisplayName(name: String): Sea {
            // [개념] firstOrNull { }은 Swift의 first(where:)와 동일합니다.
            //        조건을 만족하는 첫 번째 요소를 반환하며, 없으면 null을 반환합니다.
            //        ?: 는 Elvis 연산자로, null일 때 대체값을 지정합니다. Swift의 ?? 와 동일합니다.
            return entries.firstOrNull { it.displayName == name } ?: NONE
        }
    }
}

/**
 * 공통 지역 데이터 엔티티.
 *
 * 해역 그룹, 지역 코드, 위/경도, 수심 층별 보유 여부 등의 속성을 포함합니다.
 * iOS의 Region struct와 동일합니다.
 *
 * @property regionGroup 해역 그룹명 (예: "서해", "동해", "남해", "제주").
 * @property regionCode 지역 코드.
 * @property regionName 지역명.
 * @property latitude 위도 문자열.
 * @property longitude 경도 문자열.
 * @property hasSurface 표층 데이터 보유 여부.
 * @property hasMiddle 중층 데이터 보유 여부.
 * @property hasBottom 저층 데이터 보유 여부.
 * @property surfaceDepth 표층 깊이.
 * @property middleDepth 중층 깊이.
 * @property bottomDepth 저층 깊이.
 */
data class Region(
    val regionGroup: String,
    val regionCode: String,
    val regionName: String,
    val latitude: String,
    val longitude: String,
    val hasSurface: Boolean,
    val hasMiddle: Boolean,
    val hasBottom: Boolean,
    val surfaceDepth: String,
    val middleDepth: String,
    val bottomDepth: String
) {
    /**
     * regionGroup(한글) → Sea enum 변환.
     *
     * [개념] when은 Swift의 switch와 동일합니다.
     *        Kotlin의 when은 표현식(expression)으로도 사용되어 값을 반환할 수 있습니다.
     *
     * 제주는 남해(SOUTH)로 간주합니다.
     */
    fun toSea(): Sea = when (regionGroup) {
        "서해" -> Sea.WEST
        "동해" -> Sea.EAST
        "남해", "제주" -> Sea.SOUTH
        else -> Sea.NONE
    }
}
