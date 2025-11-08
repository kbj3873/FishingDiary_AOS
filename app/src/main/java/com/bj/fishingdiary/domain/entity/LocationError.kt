package com.bj.fishingdiary.domain.entity

/**
 * 위치 추적 에러
 * Location Tracking Error
 *
 * iOS의 LocationError enum에 대응
 */
sealed class LocationError : Exception() {
    /**
     * 위치 추적 실패
     * Location tracking failed
     */
    data object TrackingError : LocationError() {
        override val message: String
            get() = "Location tracking failed"
    }

    /**
     * 권한 거부
     * Permission denied
     */
    data object PermissionDenied : LocationError() {
        override val message: String
            get() = "Location permission denied"
    }

    /**
     * 위치 서비스 비활성화
     * Location service disabled
     */
    data object ServiceDisabled : LocationError() {
        override val message: String
            get() = "Location service is disabled"
    }
}
