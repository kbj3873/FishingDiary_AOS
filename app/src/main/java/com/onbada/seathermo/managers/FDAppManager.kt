package com.onbada.seathermo.managers

import android.content.Context
import android.content.SharedPreferences
import com.onbada.seathermo.domain.entity.MapType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

        // SharedPreferences 파일명 및 키
        // [개념] Android의 SharedPreferences는 iOS의 UserDefaults에 대응합니다.
        //        키-값 쌍으로 앱 설정을 영구 저장하며, 앱 재시작 후에도 유지됩니다.
        private const val PREFS_NAME = "seathermo_prefs"
        private const val KEY_MAP_TYPE = "map_type"
        private const val DEFAULT_MAP_TYPE_RAW_VALUE = 0

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
        const val SPEED_THRESHOLD_HIGH = 2.0 // 2노트 이상: 이동 중 (Moving)
        const val SPEED_THRESHOLD_LOW = 0.5  // 0.5~2노트: 탐색 중 (Drifting)
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
     * 현재 지도 타입.
     *
     * 기본값: GOOGLE_MAP
     * iOS의 var mapTp에 대응합니다.
     */
    var mapType: MapType = MapType.GOOGLE_MAP
        private set

    // 지도 타입 변경을 실시간으로 구독할 수 있는 StateFlow.
    // [개념] StateFlow는 항상 최신 값을 보유하는 스트림으로, collect하는 Composable이
    //        자동으로 Recomposition됩니다. HistoryDetailScreen에서 설정 변경을 감지하는 데 사용합니다.
    private val _mapTypeFlow = MutableStateFlow(MapType.GOOGLE_MAP)
    val mapTypeFlow: StateFlow<MapType> = _mapTypeFlow.asStateFlow()

    // 낚시 기록 진행 여부.
    // [개념] SettingViewModel에서 기록 중 지도 타입 변경을 차단하기 위해 확인합니다.
    var isRecording: Boolean = false
        private set

    /**
     * SharedPreferences 인스턴스 (appInitialize 호출 후 사용 가능).
     *
     * [개념] applicationContext를 저장해 두면 Activity/Fragment 생명주기와 무관하게
     *        앱이 살아있는 동안 SharedPreferences에 접근할 수 있습니다.
     *        iOS의 UserDefaults.standard에 대응합니다.
     */
    private var prefs: SharedPreferences? = null

    // ==================== 초기화 ====================

    /**
     * 앱 초기화 — Application.onCreate()에서 반드시 호출합니다.
     *
     * iOS의 FDAppManager.init()에서 UserDefaults로 저장값을 로드하는 로직에 대응합니다.
     * SharedPreferences에서 저장된 지도 타입을 읽어 mapType을 복원합니다.
     *
     * @param context Application Context
     */
    fun appInitialize(context: Context) {
        // [개념] applicationContext는 앱 전체 생명주기를 가진 Context입니다.
        //        Activity Context를 저장하면 메모리 누수가 발생할 수 있으므로 반드시 applicationContext를 사용합니다.
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // SharedPreferences에서 저장된 지도 타입 로드
        // iOS: let savedMapType = FDUserDefaults.integer(forKey: UserDefaultKey.mapType)
        // 기본값 0 = GOOGLE_MAP (신규 설치 시 구글맵이 기본값)
        val savedRawValue = prefs?.getInt(KEY_MAP_TYPE, DEFAULT_MAP_TYPE_RAW_VALUE) ?: DEFAULT_MAP_TYPE_RAW_VALUE
        setMapType(savedRawValue)
    }

    /**
     * 지도 타입을 변경하고 SharedPreferences에 저장합니다.
     *
     * iOS의 mapTp didSet { FDUserDefaults.set(mapTp.rawValue, forKey: ...) }에 대응합니다.
     *
     * @param rawValue 지도 타입 정수값 (0: Google Map, 1: Kakao Map)
     */
    fun setMapType(rawValue: Int) {
        val normalizedRawValue = when (rawValue) {
            0, 1 -> rawValue
            else -> DEFAULT_MAP_TYPE_RAW_VALUE
        }

        mapType = when (normalizedRawValue) {
            0 -> MapType.GOOGLE_MAP
            1 -> MapType.KAKAO_MAP
            else -> MapType.GOOGLE_MAP
        }

        // mapTypeFlow도 함께 업데이트하여 구독 중인 Composable에 변경을 전파합니다.
        _mapTypeFlow.value = mapType

        // [개념] SharedPreferences.edit().putInt().apply()는 비동기로 디스크에 저장합니다.
        //        apply()는 commit()과 달리 메인 스레드를 블로킹하지 않습니다.
        //        iOS의 UserDefaults.standard.set(_:forKey:)에 대응합니다.
        prefs?.edit()?.putInt(KEY_MAP_TYPE, normalizedRawValue)?.apply()
    }

    /**
     * 낚시 기록 진행 여부를 설정합니다.
     *
     * FishingRecordViewModel에서 기록 시작/종료 시 호출합니다.
     * SettingViewModel이 기록 중 지도 타입 변경을 차단하는 데 사용합니다.
     *
     * @param recording true = 기록 중, false = 기록 종료
     */
    fun setRecording(recording: Boolean) {
        isRecording = recording
    }
}
