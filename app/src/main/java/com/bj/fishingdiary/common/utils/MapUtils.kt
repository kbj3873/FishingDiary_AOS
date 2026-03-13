package com.bj.fishingdiary.common.utils

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * 지도 관련 유틸리티 클래스
 * Map Related Utility Class
 *
 * iOS의 String+Extension (DMSType, DtoDM, DtoDMS) 기능 대응
 */
object MapUtils {

    /**
     * Degree -> DM (도분) 변환
     * 128.123456 -> 128° 7.4074'
     */
    fun convertDtoDM(coordinate: Double): String {
        val degrees = floor(coordinate).toInt()
        val minutes = (coordinate - degrees) * 60
        
        // 소수점 4자리까지 표시
        return String.format("%d° %.4f'", degrees, minutes)
    }

    /**
     * Degree -> DMS (도분초) 변환
     * 128.123456 -> 128° 7' 24.44"
     */
    fun convertDtoDMS(coordinate: Double): String {
        val degrees = floor(coordinate).toInt()
        val remainingMinutes = (coordinate - degrees) * 60
        val minutes = floor(remainingMinutes).toInt()
        val seconds = (remainingMinutes - minutes) * 60
        
        // 소수점 2자리까지 표시
        return String.format("%d° %d' %.2f\"", degrees, minutes, seconds)
    }

    /**
     * DMS -> Degree 변환
     * (현재 iOS 코드에는 String.DMStoD가 있지만 구현 내용이 비어있음,
     *  필요 시 구현)
     */
}
