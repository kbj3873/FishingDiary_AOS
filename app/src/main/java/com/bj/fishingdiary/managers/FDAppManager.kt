package com.bj.fishingdiary.managers

import android.content.Context
import com.bj.fishingdiary.domain.entity.MapType

/**
 * 앱 관리자 클래스
 * App Manager Class
 *
 * iOS의 FDAppManager.swift에 대응
 * 앱 전역 설정 및 초기화를 담당
 */
class FDAppManager private constructor() {

    companion object {
        /**
         * Singleton 인스턴스
         * iOS의 static let shared와 동일
         */
        @Volatile
        private var instance: FDAppManager? = null

        fun getInstance(): FDAppManager {
            return instance ?: synchronized(this) {
                instance ?: FDAppManager().also { instance = it }
            }
        }

        // ==================== 상수 정의 ====================

        /**
         * 저장할 포인트 개수 단위
         * Save points count unit
         *
         * 이 개수만큼 위치 데이터가 쌓이면 파일에 저장
         */
        const val SAVE_FOR_POINTS: Int = 200

        /**
         * 1 knot = 1.852 km/h
         * Knot to km/h conversion factor
         */
        const val KMH_KNOT: Float = 1.852f

        /**
         * 포인트 구간 판단 속도 기준
         * Point area velocity threshold
         *
         * 이 속도 미만일 경우 포인트 구간으로 간주
         * Below this velocity is considered as a point area
         */
        const val POINT_VELOCITY: Float = 2 * KMH_KNOT  // 약 3.7 km/h
    }

    // ==================== 속성 ====================

    /**
     * 현재 지도 타입
     * Current map type
     *
     * 기본값: APPLE_MAP (Android에서는 Google Maps)
     */
    var mapType: MapType = MapType.APPLE_MAP
        private set

    // ==================== 초기화 ====================

    /**
     * 앱 초기화
     * App Initialization
     *
     * iOS의 appInitialize()에 대응
     * 기본 디렉토리 생성 등 초기 설정 수행
     *
     * @param context Application Context
     */
    fun appInitialize(context: Context) {
        // 기본 디렉토리 생성
        FDFileManager.getInstance(context).createDefaultDirectories()
    }

    /**
     * 지도 타입 설정
     * Set Map Type
     *
     * @param rawValue 지도 타입 정수값 (0: Apple/Google Map, 1: Kakao Map)
     */
    fun setMapType(rawValue: Int) {
        mapType = when (rawValue) {
            0 -> MapType.APPLE_MAP
            1 -> MapType.KAKAO_MAP
            else -> MapType.APPLE_MAP
        }
    }
}
