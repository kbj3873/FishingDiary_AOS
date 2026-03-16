package com.onbada.seathermo.data.network.datamapping

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.onbada.seathermo.domain.entity.SeaAnalysisQuery
import com.onbada.seathermo.domain.entity.WeeklyTemperature
import java.lang.reflect.Type

// ==================== Request DTO ====================

/**
 * 수온 이력 조회 Request DTO (NIFS 웹 크롤링 우회 API).
 *
 * form-encoded 방식으로 전송합니다 (AsciiBodyEncoder 사용).
 * iOS의 RisaInfoListRequestDTO에 대응합니다.
 *
 * [개념] val로 선언한 프로퍼티는 불변이며, 초기화 후 변경 불가합니다.
 *        Swift의 let 상수와 동일합니다.
 */
data class RisaInfoListRequestDTO(
    val obsrvnGroupNm: String,
    val obsvtrCd: String,
    val obsFrom: String,
    val obsTo: String,
    val ord: String,
    val ordType: String,
    val rowCountPage: String,
    // 고정값 파라미터
    val rstSel: String = "on",
    val obsTimeFrom: String = "0000",
    val obsTimeTo: String = "2330",
    val obsTimeDefault: String = "N",
    val selectPage: String = "1"
) {
    constructor(query: SeaAnalysisQuery) : this(
        obsrvnGroupNm = query.gruNam,
        obsvtrCd = query.staCde,
        obsFrom = query.obsFrom,
        obsTo = query.obsTo,
        ord = query.ord,
        ordType = query.ordType,
        // dataCnt가 비어 있으면 1000으로 기본값 설정 (7일 전체 조회)
        rowCountPage = query.dataCnt.ifEmpty { "1000" }
    )

    /**
     * DTO를 form-encoded body 파라미터 Map으로 변환합니다.
     *
     * [개념] "rst-sel" 처럼 하이픈이 포함된 키는 Kotlin 프로퍼티명으로 쓸 수 없어
     *        Map에서 문자열 키로 직접 지정합니다.
     *        iOS의 CodingKeys case rstSel = "rst-sel"에 대응합니다.
     */
    fun toBodyParams(): Map<String, Any> = mapOf(
        "obsrvnGroupNm" to obsrvnGroupNm,
        "obsvtrCd" to obsvtrCd,
        "obsFrom" to obsFrom,
        "obsTo" to obsTo,
        "ord" to ord,
        "ordType" to ordType,
        "rowCountPage" to rowCountPage,
        "rst-sel" to rstSel,
        "obsTimeFrom" to obsTimeFrom,
        "obsTimeTo" to obsTimeTo,
        "obsTimeDefault" to obsTimeDefault,
        "selectPage" to selectPage
    )
}

// ==================== Response DTO ====================

/**
 * 수온 이력 조회 Response DTO (최상위 래퍼).
 *
 * iOS의 RisaInfoResponseDTO에 대응합니다.
 */
data class RisaInfoResponseDTO(
    @SerializedName("retList") val retList: List<RisaInfoItemDTO>
)

/**
 * 수온 이력 1건 DTO.
 *
 * [개념] NIFS API는 동일 필드를 상황에 따라 String 또는 Number로 반환합니다.
 *        Gson의 기본 파서는 타입 불일치 시 예외를 발생시키므로
 *        @JsonAdapter로 커스텀 역직렬화 로직을 지정합니다.
 *        iOS의 init(from decoder:) 커스텀 디코더에 대응합니다.
 *
 * iOS의 RisaInfoItemDTO에 대응합니다.
 */
@JsonAdapter(RisaInfoItemDTO.Deserializer::class)
data class RisaInfoItemDTO(
    val obsrvnDt: String,       // 관측 일시 "2026-02-21 10:00"
    val wtrTempS: String,       // 표층 수온
    val wtrTempM: String,       // 중층 수온
    val wtrTempB: String,       // 저층 수온
    val obsvtrKornNm: String,   // 관측소 한글명
    val obsvtrNm: String?,      // 관측소명(코드) 형식 (옵셔널)
    val obsvtrCd: String?,      // 관측소 코드 (옵셔널)
    val lat: String?,           // 위도 (String 또는 Double로 올 수 있음)
    val lot: String?,           // 경도 (String 또는 Double로 올 수 있음)
    val sfclyrDpwt: Int?,       // 표층 수심 (Int 또는 String으로 올 수 있음)
    val mlyrDpwt: String?,      // 중층 수심
    val btmlyrDpwt: String?     // 저층 수심
) {
    /**
     * Gson 커스텀 역직렬화 클래스.
     *
     * [개념] inner class 대신 중첩 class(nested class)로 선언합니다.
     *        Kotlin에서 class 안의 class는 기본적으로 static(외부 참조 없음)입니다.
     *        Swift의 nested type과 동일합니다.
     *
     *        JsonDeserializer<T>는 Gson의 역직렬화 인터페이스입니다.
     *        deserialize() 하나만 구현하면 됩니다.
     */
    class Deserializer : JsonDeserializer<RisaInfoItemDTO> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): RisaInfoItemDTO {
            val obj = json.asJsonObject

            // [개념] asStringOrNull은 아래에 정의된 확장 함수입니다.
            //        JsonElement가 null이거나 JsonNull이면 null, 그 외는 문자열로 변환합니다.
            //        Gson의 asString은 String/Number/Boolean 모두 문자열로 변환해줍니다.
            return RisaInfoItemDTO(
                obsrvnDt = obj["obsrvnDt"].asStringOrNull() ?: "",
                wtrTempS = obj["wtrTempS"].asStringOrNull() ?: "",
                wtrTempM = obj["wtrTempM"].asStringOrNull() ?: "",
                wtrTempB = obj["wtrTempB"].asStringOrNull() ?: "",
                obsvtrKornNm = obj["obsvtrKornNm"].asStringOrNull() ?: "",
                obsvtrNm = obj["obsvtrNm"].asStringOrNull(),
                obsvtrCd = obj["obsvtrCd"].asStringOrNull(),
                lat = obj["lat"].asStringOrNull(),
                lot = obj["lot"].asStringOrNull(),
                // [개념] toIntOrNull()은 변환 실패 시 null을 반환합니다.
                //        Swift의 Int(string) 옵셔널 이니셜라이저와 동일합니다.
                sfclyrDpwt = obj["sfclyrDpwt"].asStringOrNull()?.toIntOrNull(),
                mlyrDpwt = obj["mlyrDpwt"].asStringOrNull(),
                btmlyrDpwt = obj["btmlyrDpwt"].asStringOrNull()
            )
        }

        // JsonElement → String? 변환 확장 함수 (null-safe).
        // [개념] 확장 함수(Extension Function)는 기존 클래스를 수정하지 않고 새 메서드를 추가합니다.
        //        Swift의 extension과 동일합니다.
        //        JsonElement?에서 ?는 nullable 수신자를 의미하며, this가 null이면 null을 반환합니다.
        private fun JsonElement?.asStringOrNull(): String? {
            if (this == null || isJsonNull) return null
            return asString
        }
    }

    /**
     * DTO를 도메인 엔티티로 변환합니다.
     *
     * iOS의 toDomain() 변환 로직을 그대로 이식합니다.
     * 99.9 이상의 값은 API 에러 응답값이므로 -99.0으로 치환합니다.
     */
    fun toDomain(): WeeklyTemperature {
        // 날짜 문자열에서 숫자만 추출해 Long으로 변환 ("2026-02-21 10:00" → 202602211000).
        // [개념] filter { }는 조건을 만족하는 요소만 남깁니다. Swift의 filter와 동일합니다.
        //        isDigit()은 해당 문자가 숫자(0~9)인지 확인합니다.
        val dateT = obsrvnDt.filter { it.isDigit() }.toLongOrNull() ?: 0L

        // [개념] toFloatOrNull()은 변환 실패 시 null 반환 후 ?: 로 기본값(-99.0f) 적용.
        val floatS = wtrTempS.toFloatOrNull() ?: -99.0f
        val floatM = wtrTempM.toFloatOrNull() ?: -99.0f
        val floatB = wtrTempB.toFloatOrNull() ?: -99.0f
        val floatLat = lat?.toFloatOrNull() ?: -99.0f
        val floatLon = lot?.toFloatOrNull() ?: -99.0f
        val sDepth = if ((sfclyrDpwt?.toFloat() ?: 0f) > 0f) sfclyrDpwt!!.toFloat() else -99.0f
        val mDepth = mlyrDpwt?.toFloatOrNull() ?: -99.0f
        val bDepth = btmlyrDpwt?.toFloatOrNull() ?: -99.0f

        return WeeklyTemperature(
            staCde = obsvtrCd ?: "",
            staNamKor = obsvtrKornNm,
            staNam = obsvtrNm ?: "",
            obsDtm = obsrvnDt,
            wtrTempS = if (floatS > 90f) -99.0f else floatS,
            surDep = sDepth,
            wtrTempM = if (floatM > 90f) -99.0f else floatM,
            midDep = mDepth,
            wtrTempB = if (floatB > 90f) -99.0f else floatB,
            botDep = bDepth,
            lon = floatLon,
            lat = floatLat,
            dateT = dateT
        )
    }
}
