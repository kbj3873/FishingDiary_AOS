package com.onbada.seathermo.managers

import android.content.Context
import com.onbada.seathermo.domain.entity.MapType

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

        // [추가] 낚시 상태 판별을 위한 속도 임계값 (knots)
        const val SPEED_THRESHOLD_HIGH = 3.0 // 3노트 이상: 이동 중 (Moving)
        const val SPEED_THRESHOLD_LOW = 0.5  // 0.5~3노트: 탐색 중 (Drifting)
        // 0.5노트 미만: 낚시 중 (Fishing)
    }

    /**
     * 낚시 상태 정의
     * Fishing State Definition
     *
     * iOS의 FDAppManager.FishingState에 대응
     */
    enum class FishingState(val description: String, val value: Int) {
        MOVING("이동 중", 0),
        DRIFTING("탐색 중", 1),
        FISHING("낚시 중", 2)
    }

    // ==================== 속성 ====================

    /**
     * 현재 지도 타입
     * Current map type
     *
     * 기본값: APPLE_MAP (Android에서는 Google Maps)
     */
    var mapType: MapType = MapType.GOOGLE_MAP
        private set

    // ==================== 초기화 ====================

    /**
     * 앱 초기화.
     *
     * iOS의 appInitialize()에 대응합니다.
     *
     * @param context Application Context.
     *                [개념] Android의 Context는 앱 환경 정보(파일 경로, 리소스 등)에 접근하는 핵심 객체입니다.
     *                Swift에는 대응 개념이 없으며, iOS에서는 번들/파일 시스템에 직접 접근합니다.
     */
    fun appInitialize(context: Context) {
        // TODO: 초기화 로직 추가 예정
    }

    /**
     * 지도 타입 설정
     * Set Map Type
     *
     * @param rawValue 지도 타입 정수값 (0: Google Map, 1: Kakao Map)
     */
    fun setMapType(rawValue: Int) {
        mapType = when (rawValue) {
            0 -> MapType.GOOGLE_MAP
            1 -> MapType.KAKAO_MAP
            else -> MapType.GOOGLE_MAP
        }
    }
}
