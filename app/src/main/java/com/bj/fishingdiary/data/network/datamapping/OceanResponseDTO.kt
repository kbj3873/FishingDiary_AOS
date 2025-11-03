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
        val wtrTempS: Float?,

        @SerializedName("SUR_DEP")
        val surDep: Float?,

        @SerializedName("WTR_TEMP_M")
        val wtrTempM: Float?,

        @SerializedName("MID_DEP")
        val midDep: Float?,

        @SerializedName("WTR_TEMP_B")
        val wtrTempB: Float?,

        @SerializedName("BOT_DEP")
        val botDep: Float?,

        @SerializedName("LON")
        val lon: Float?,

        @SerializedName("LAT")
        val lat: Float?
    ) {
        /**
         * 날짜 정수 값 계산
         * Calculate date integer value
         *
         * iOS와 동일한 로직: obsDtm에서 숫자만 추출하여 Int로 변환
         */
        val dateT: Int
            get() {
                val str = obsDtm?.filter { it.isDigit() } ?: ""
                return str.toIntOrNull() ?: -1
            }

        fun toDomain(): Ocean {
            return Ocean(
                staCde = staCde ?: "",
                staNamKor = staNamKor ?: "",
                staNam = staNam ?: "",
                obsDtm = obsDtm ?: "",
                wtrTempS = wtrTempS ?: -99f,
                surDep = surDep ?: -99f,
                wtrTempM = wtrTempM ?: -99f,
                midDep = midDep ?: -99f,
                wtrTempB = wtrTempB ?: -99f,
                botDep = botDep ?: -99f,
                lon = lon ?: -99f,
                lat = lat ?: -99f,
                dateT = dateT
            )
        }
    }
}
