package com.onbada.seathermo.data.network.datamapping

import com.google.gson.annotations.SerializedName
import com.onbada.seathermo.domain.entity.CurrentTemperature
import com.onbada.seathermo.domain.entity.CurrentTemperatureQuery

// ==================== Request DTO ====================

/**
 * 실시간 수온 목록 조회 Request DTO.
 *
 * [개념] @SerializedName은 Gson에서 JSON 필드명과 Kotlin 프로퍼티명이 다를 때 매핑을 지정합니다.
 *        Swift의 CodingKeys enum과 동일한 역할입니다.
 *        API는 snake_case(gru_nam)를 사용하지만 Kotlin 코드는 camelCase(gruNam)를 사용합니다.
 *
 * iOS의 RisaListRequestDTO에 대응합니다.
 */
data class RisaListRequestDTO(
    @SerializedName("key") val key: String,
    @SerializedName("id") val id: String,
    @SerializedName("gru_nam") val gruNam: String
) {
    // CurrentTemperatureQuery 도메인 모델로부터 DTO를 생성하는 보조 생성자.
    // [개념] constructor(...)는 추가 생성자(secondary constructor)입니다.
    //        Swift의 convenience init에 대응합니다.
    constructor(query: CurrentTemperatureQuery) : this(
        key = query.key,
        id = query.id,
        gruNam = query.gruNam
    )

    // DTO를 Map으로 변환합니다 (QueryString 파라미터 전달용).
    // [개념] mapOf()는 불변 Map을 생성합니다. Swift의 Dictionary literal과 동일합니다.
    fun toQueryParams(): Map<String, Any> = mapOf(
        "key" to key,
        "id" to id,
        "gru_nam" to gruNam
    )
}

// ==================== Response DTO ====================

/**
 * 실시간 수온 목록 조회 Response DTO (최상위 래퍼).
 *
 * iOS의 RisaListResponseDTO에 대응합니다.
 */
data class RisaListResponseDTO(
    @SerializedName("header") val header: RisaListHeaderDTO,
    @SerializedName("body") val body: RisaListBodyDTO
)

data class RisaListHeaderDTO(
    @SerializedName("resultCode") val resultCode: String,
    @SerializedName("resultMsg") val resultMsg: String
)

data class RisaListBodyDTO(
    // [개념] List<T>? 는 리스트 자체가 null일 수 있는 nullable 타입입니다.
    //        Swift의 [RisaListItemDTO]? 와 동일합니다.
    @SerializedName("item") val item: List<RisaListItemDTO>?
)

/**
 * 수온 측정소 1건 DTO.
 *
 * iOS의 RisaListItemDTO에 대응합니다.
 *
 * @property gruNam  해역 그룹명 (예: "서해")
 * @property staCde  측정소 코드
 * @property obsLay  관측 층 (표층/중층/저층)
 * @property staNamKor 측정소 한글명
 * @property wtrTmp  수온 문자열 (예: "12.5")
 */
data class RisaListItemDTO(
    @SerializedName("gru_nam") val gruNam: String?,
    @SerializedName("sta_cde") val staCde: String?,
    @SerializedName("obs_lay") val obsLay: String?,
    @SerializedName("sta_nam_kor") val staNamKor: String?,
    @SerializedName("wtr_tmp") val wtrTmp: String?
) {
    /**
     * DTO를 도메인 엔티티로 변환합니다.
     *
     * [개념] ?: (Elvis 연산자)는 좌변이 null이면 우변 값을 반환합니다.
     *        Swift의 ?? (nil 병합 연산자)와 동일합니다.
     */
    fun toDomain(): CurrentTemperature = CurrentTemperature(
        gruNam = gruNam ?: "",
        staCde = staCde ?: "",
        obsLay = obsLay ?: "",
        staNamKor = staNamKor ?: "",
        wtrTmp = wtrTmp ?: ""
    )
}
