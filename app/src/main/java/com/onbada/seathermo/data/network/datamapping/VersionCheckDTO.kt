package com.onbada.seathermo.data.network.datamapping

import com.google.gson.annotations.SerializedName
import com.onbada.seathermo.domain.entity.VersionStatus

// ==================== Request DTO ====================

/**
 * 버전 체크 Request DTO.
 *
 * iOS의 VersionCheckRequestDTO에 대응합니다.
 */
data class VersionCheckRequestDTO(
    // [개념] @SerializedName으로 JSON 키를 snake_case로 지정합니다.
    //        API는 "app_version"을 요구하지만 Kotlin 프로퍼티는 camelCase가 관례입니다.
    @SerializedName("app_version") val appVersion: String
) {
    fun toBodyParams(): Map<String, Any> = mapOf("app_version" to appVersion)
}

// ==================== Response DTO ====================

/**
 * 버전 체크 Response DTO (온바다 서버 공통 래퍼).
 *
 * iOS의 VersionCheckResponseDTO에 대응합니다.
 */
data class VersionCheckResponseDTO(
    @SerializedName("resultCode") val resultCode: Int,
    @SerializedName("resultMsg") val resultMsg: String,
    // [개념] data?: 은 응답에 data 필드가 없을 수도 있음을 표현합니다 (nullable).
    //        Swift의 let data: VersionCheckDataDTO? 와 동일합니다.
    @SerializedName("data") val data: VersionCheckDataDTO?
) {
    /**
     * DTO를 도메인 엔티티로 변환합니다.
     *
     * data가 null이면 빈 VersionStatus를 반환합니다.
     * iOS의 extension VersionCheckResponseDTO { func toDomain() }에 대응합니다.
     */
    fun toDomain(): VersionStatus {
        val d = data ?: return VersionStatus(
            minimumVersion = "",
            latestVersion = "",
            forceUpdate = false,
            needUpdate = false,
            message = ""
        )
        return VersionStatus(
            minimumVersion = d.minimumVersion,
            latestVersion = d.latestVersion,
            forceUpdate = d.forceUpdate,
            needUpdate = d.needUpdate,
            message = d.message
        )
    }
}

/**
 * 버전 상세 데이터 DTO.
 *
 * iOS의 VersionCheckDataDTO에 대응합니다.
 */
data class VersionCheckDataDTO(
    @SerializedName("minimum_version") val minimumVersion: String,
    @SerializedName("latest_version") val latestVersion: String,
    @SerializedName("force_update") val forceUpdate: Boolean,
    @SerializedName("need_update") val needUpdate: Boolean,
    @SerializedName("message") val message: String
)
