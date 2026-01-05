package com.bj.fishingdiary.data.network.datamapping

import com.bj.fishingdiary.domain.entity.*
import com.google.gson.annotations.SerializedName

/**
 * 헤더 DTO
 * Header DTO
 *
 * iOS의 HeaderDTO에 대응
 */
data class HeaderDTO(
    @SerializedName("resultCode")
    val resultCode: String,

    @SerializedName("resultMsg")
    val resultMsg: String
) {
    /**
     * DTO를 Domain 모델로 변환
     * Convert DTO to Domain model
     */
    fun toDomain(): Header {
        return Header(
            resultCode = resultCode,
            resultMsg = resultMsg
        )
    }
}

// ==================== RISA List Response ====================

/**
 * RISA List 응답 DTO
 * RISA List Response DTO
 *
 * iOS의 RisaListResponseDTO에 대응
 */
data class RisaListResponseDTO(
    @SerializedName("header")
    val header: HeaderDTO,

    @SerializedName("body")
    val body: RisaListBodyDTO
) {
    fun toDomain(): RisaResponse<RisaList> {
        return RisaResponse(
            header = header.toDomain(),
            body = body.toDomain()
        )
    }

    /**
     * RISA List Body DTO
     */
    data class RisaListBodyDTO(
        @SerializedName("item")
        val item: List<RisaListItemDTO>
    ) {
        fun toDomain(): RisaBody<RisaList> {
            return RisaBody(
                item = item.map { it.toDomain() }
            )
        }
    }

    /**
     * RISA List Item DTO
     */
    data class RisaListItemDTO(
        @SerializedName("gru_nam")
        val gruNam: String?,

        @SerializedName("sta_cde")
        val staCde: String?,

        @SerializedName("obs_lay")
        val obsLay: String?,

        @SerializedName("sta_nam_kor")
        val staNamKor: String?,

        @SerializedName("wtr_tmp")
        val wtrTmp: String?
    ) {
        fun toDomain(): RisaList {
            return RisaList(
                gruNam = gruNam ?: "",
                staCde = staCde ?: "",
                obsLay = obsLay ?: "",
                staNamKor = staNamKor ?: "",
                wtrTmp = wtrTmp ?: ""
            )
        }
    }
}

// ==================== RISA Code Response ====================

/**
 * RISA Code 응답 DTO
 * RISA Code Response DTO
 *
 * iOS의 RisaCodeResponseDTO에 대응
 */
data class RisaCodeResponseDTO(
    @SerializedName("header")
    val header: HeaderDTO,

    @SerializedName("body")
    val body: RisaCodeBodyDTO
) {
    fun toDomain(): RisaResponse<RisaCode> {
        return RisaResponse(
            header = header.toDomain(),
            body = body.toDomain()
        )
    }

    /**
     * RISA Code Body DTO
     */
    data class RisaCodeBodyDTO(
        @SerializedName("item")
        val item: List<RisaCodeItemDTO>
    ) {
        fun toDomain(): RisaBody<RisaCode> {
            return RisaBody(
                item = item.map { it.toDomain() }
            )
        }
    }

    /**
     * RISA Code Item DTO
     */
    data class RisaCodeItemDTO(
        @SerializedName("gru_nam")
        val gruNam: String?,

        @SerializedName("sta_cde")
        val staCde: String?,

        @SerializedName("sta_nam_kor")
        val staNamKor: String?
    ) {
        fun toDomain(): RisaCode {
            return RisaCode(
                gruNam = gruNam ?: "",
                staCde = staCde ?: "",
                staNamKor = staNamKor ?: ""
            )
        }
    }
}

// ==================== COO List Response ====================

/**
 * COO List 응답 DTO
 * COO List Response DTO
 *
 * iOS의 CooListResponseDTO에 대응
 */
data class CooListResponseDTO(
    @SerializedName("header")
    val header: HeaderDTO,

    @SerializedName("body")
    val body: CooListBodyDTO
) {
    fun toDomain(): RisaResponse<RisaCoo> {
        return RisaResponse(
            header = header.toDomain(),
            body = body.toDomain()
        )
    }

    /**
     * COO List Body DTO
     */
    data class CooListBodyDTO(
        @SerializedName("item")
        val item: List<CooListItemDTO>
    ) {
        fun toDomain(): RisaBody<RisaCoo> {
            return RisaBody(
                item = item.map { it.toDomain() }
            )
        }
    }

    /**
     * COO List Item DTO
     */
    data class CooListItemDTO(
        @SerializedName("gru_nam")
        val gruNam: String?,

        @SerializedName("sta_cde")
        val staCde: String?,

        @SerializedName("sta_nam_kor")
        val staNamKor: String?,

        @SerializedName("obs_dat")
        val obsDate: String?,

        @SerializedName("wtr_tmp")
        val wtrTmp: String?
    ) {
        fun toDomain(): RisaCoo {
            return RisaCoo(
                gruNam = gruNam ?: "",
                staCde = staCde ?: "",
                staNamKor = staNamKor ?: "",
                obsDate = obsDate ?: "",
                wtrTmp = wtrTmp ?: ""
            )
        }
    }
}

// ==================== Ocean Response ====================

/**
 * Ocean 응답 DTO
 * Ocean Response DTO
 *
 * iOS의 OceanResponseDTO에 대응
 */
data class OceanResponseDTO(
    @SerializedName("list")
    val list: List<OceanDTO>
) {
    fun toDomain(): OceanResponse {
        return OceanResponse(
            list = list.map { it.toDomain() }
        )
    }

    /**
     * Ocean DTO
     *
     * iOS의 OceanDTO에 대응
     *
     * 주의: 서버에서 값이 없을 때 빈 문자열("")로 응답하는 경우가 있음
     * 예: "WTR_TEMP_S":15.1 (값 있음) vs "MID_DEP":"" (값 없음)
     * 따라서 온도/깊이 필드를 String으로 받아서 toDomain()에서 변환
     */
    data class OceanDTO(
        @SerializedName("STA_CDE")
        val staCde: String?,

        @SerializedName("STA_NAM_KOR")
        val staNamKor: String?,

        @SerializedName("STA_NAM")
        val staNam: String?,

        @SerializedName("OBS_DTM")
        val obsDtm: String?,

        @SerializedName("WTR_TEMP_S")
        val wtrTempS: String?,

        @SerializedName("SUR_DEP")
        val surDep: String?,

        @SerializedName("WTR_TEMP_M")
        val wtrTempM: String?,

        @SerializedName("MID_DEP")
        val midDep: String?,

        @SerializedName("WTR_TEMP_B")
        val wtrTempB: String?,

        @SerializedName("BOT_DEP")
        val botDep: String?,

        @SerializedName("LON")
        val lon: Float?,

        @SerializedName("LAT")
        val lat: Float?
    ) {
        /**
         * 날짜 정수 값 계산
         * Calculate date integer value
         *
         * iOS와 동일한 로직: obsDtm에서 숫자만 추출하여 Long으로 변환
         * "2025-12-20 00:00" -> "202512200000" (Long)
         *
         * Int 대신 Long을 사용하는 이유:
         * - Int 최대값: 2,147,483,647
         * - "202512200000"은 Int 범위를 초과
         * - Swift의 Int는 64비트이지만 Kotlin의 Int는 32비트
         */
        val dateT: Long
            get() {
                val str = obsDtm?.filter { it.isDigit() } ?: ""
                return str.toLongOrNull() ?: -1L
            }

        /**
         * String을 Float로 안전하게 변환
         * Safely convert String to Float
         *
         * @param value String 값 (null 또는 빈 문자열 가능)
         * @return Float 값 (변환 실패 시 -99f)
         */
        private fun String?.toFloatOrDefault(): Float {
            return this?.toFloatOrNull() ?: -99f
        }

        fun toDomain(): Ocean {
            return Ocean(
                staCde = staCde ?: "",
                staNamKor = staNamKor ?: "",
                staNam = staNam ?: "",
                obsDtm = obsDtm ?: "",
                wtrTempS = wtrTempS.toFloatOrDefault(),
                surDep = surDep.toFloatOrDefault(),
                wtrTempM = wtrTempM.toFloatOrDefault(),
                midDep = midDep.toFloatOrDefault(),
                wtrTempB = wtrTempB.toFloatOrDefault(),
                botDep = botDep.toFloatOrDefault(),
                lon = lon ?: -99f,
                lat = lat ?: -99f,
                dateT = dateT
            )
        }
    }
}
