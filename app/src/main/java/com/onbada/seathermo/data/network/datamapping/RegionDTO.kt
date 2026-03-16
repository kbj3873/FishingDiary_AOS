package com.onbada.seathermo.data.network.datamapping

import com.google.gson.annotations.SerializedName
import com.onbada.seathermo.domain.entity.Region

// ==================== Response DTO ====================

/**
 * 관측소(Region) 목록 Response DTO (온바다 서버 공통 래퍼).
 *
 * iOS의 RegionResponseDTO에 대응합니다.
 */
data class RegionResponseDTO(
    @SerializedName("resultCode") val resultCode: Int,
    @SerializedName("resultMsg") val resultMsg: String,
    @SerializedName("data") val data: List<RegionItemDTO>?
)

/**
 * 관측소 1건 DTO.
 *
 * iOS의 RegionItemDTO에 대응합니다.
 *
 * [개념] API JSON 필드명이 snake_case이므로 @SerializedName으로 매핑합니다.
 *        has_surface 등은 Int(0/1)로 오며, toDomain()에서 Boolean으로 변환합니다.
 */
data class RegionItemDTO(
    @SerializedName("region_group") val regionGroup: String,
    @SerializedName("region_code") val regionCode: String,
    @SerializedName("region_name") val regionName: String,
    @SerializedName("latitude") val latitude: String,
    @SerializedName("longitude") val longitude: String,
    @SerializedName("has_surface") val hasSurface: Int,
    @SerializedName("has_middle") val hasMiddle: Int,
    @SerializedName("has_bottom") val hasBottom: Int,
    @SerializedName("surface_depth") val surfaceDepth: String,
    @SerializedName("middle_depth") val middleDepth: String,
    @SerializedName("bottom_depth") val bottomDepth: String
) {
    /**
     * DTO를 도메인 엔티티로 변환합니다.
     *
     * [개념] hasSurface == 1 처럼 비교 연산의 결과(Boolean)를 직접 프로퍼티에 할당합니다.
     *        Swift의 has_surface == 1 과 동일합니다.
     */
    fun toDomain(): Region = Region(
        regionGroup = regionGroup,
        regionCode = regionCode,
        regionName = regionName,
        latitude = latitude,
        longitude = longitude,
        hasSurface = hasSurface == 1,
        hasMiddle = hasMiddle == 1,
        hasBottom = hasBottom == 1,
        surfaceDepth = surfaceDepth,
        middleDepth = middleDepth,
        bottomDepth = bottomDepth
    )
}
