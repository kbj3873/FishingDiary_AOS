package com.bj.fishingdiary.data.network.datamapping

import com.bj.fishingdiary.domain.entity.*
import com.google.gson.annotations.SerializedName

/**
 * RISA List 요청 DTO
 * RISA List Request DTO
 *
 * iOS의 RisaListRequestDTO에 대응
 *
 * @SerializedName: JSON 직렬화 시 사용할 키 이름 지정
 * iOS의 CodingKeys와 동일한 역할
 */
data class RisaListRequestDTO(
    @SerializedName("key")
    val key: String,

    @SerializedName("id")
    val id: String,

    @SerializedName("gru_nam")
    val gruNam: String
) {
    /**
     * Domain Query에서 DTO로 변환하는 생성자
     * Constructor to convert from Domain Query to DTO
     */
    constructor(query: RisaListQuery) : this(
        key = query.key,
        id = query.id,
        gruNam = query.gruNam
    )
}

/**
 * RISA Code 요청 DTO
 * RISA Code Request DTO
 *
 * iOS의 RisaCodeRequestDTO에 대응
 */
data class RisaCodeRequestDTO(
    @SerializedName("key")
    val key: String,

    @SerializedName("id")
    val id: String,

    @SerializedName("gru_nam")
    val gruNam: String,

    @SerializedName("use_yn")
    val useYn: String
) {
    constructor(query: RisaCodeQuery) : this(
        key = query.key,
        id = query.id,
        gruNam = query.gruNam,
        useYn = query.useYn
    )
}

/**
 * RISA COO 요청 DTO
 * RISA COO Request DTO
 *
 * iOS의 RisaCooRequestDTO에 대응
 */
data class RisaCooRequestDTO(
    @SerializedName("key")
    val key: String,

    @SerializedName("id")
    val id: String,

    @SerializedName("sdate")
    val sDate: String,

    @SerializedName("edate")
    val eDate: String
) {
    constructor(query: RisaCooQuery) : this(
        key = query.key,
        id = query.id,
        sDate = query.sDate,
        eDate = query.eDate
    )
}

/**
 * Ocean 요청 DTO
 * Ocean Request DTO
 *
 * iOS의 OceanRequestDTO에 대응
 */
data class OceanRequestDTO(
    @SerializedName("id")
    val id: String,

    @SerializedName("gru_nam")
    val gruNam: String,

    @SerializedName("use_yn")
    val useYn: String,

    @SerializedName("sta_cde")
    val staCde: String,

    @SerializedName("data_cnt")
    val dataCnt: String,

    @SerializedName("ord")
    val ord: String,

    @SerializedName("ord_type")
    val ordType: String,

    @SerializedName("obs_from")
    val obsFrom: String,

    @SerializedName("obs_to")
    val obsTo: String
) {
    constructor(query: OceanQuery) : this(
        id = query.id,
        gruNam = query.gruNam,
        useYn = query.useYn,
        staCde = query.staCde,
        dataCnt = query.dataCnt,
        ord = query.ord,
        ordType = query.ordType,
        obsFrom = query.obsFrom,
        obsTo = query.obsTo
    )
}
