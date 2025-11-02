package com.bj.fishingdiary.presentation.main

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bj.fishingdiary.R

/**
 * 메인 홈 화면 Activity
 * Main Home Screen Activity
 *
 * Activity란?
 * - Android에서 화면 하나를 나타내는 기본 단위
 * - iOS의 UIViewController 또는 SwiftUI의 View와 유사한 개념
 * - AppCompatActivity를 상속받아 하위 버전 Android와의 호환성 제공
 */
class MainHomeActivity : AppCompatActivity() {

    // ===== 뷰 변수 선언 =====
    // View variable declarations

    /**
     * lateinit var: 나중에 초기화할 변수
     * - lateinit: 선언 시점에는 초기화하지 않고, 나중에(onCreate에서) 초기화
     * - var: 변경 가능한 변수 (val은 변경 불가능한 상수)
     *
     * private: 이 클래스 내부에서만 접근 가능 (캡슐화)
     */

    // 기능 버튼들
    // Function buttons
    private lateinit var btnSeaTemperature: Button  // 수온정보 버튼
    private lateinit var btnPoint: Button           // 포인트 버튼
    private lateinit var btnStartTracking: Button   // 추적시작 버튼

    // 지도 선택 버튼들
    // Map selection buttons
    private lateinit var btnAppleMap: Button        // Apple Map 버튼
    private lateinit var btnKakaoMap: Button        // Kakao Map 버튼

    /**
     * 현재 선택된 지도 타입을 저장하는 변수
     * Variable to store currently selected map type
     *
     * enum class: 정해진 값들만 가질 수 있는 타입
     * - iOS의 enum과 동일한 개념
     */
    private var selectedMapType: MapType = MapType.APPLE_MAP

    /**
     * onCreate: Activity가 생성될 때 호출되는 생명주기 메서드
     * - iOS의 viewDidLoad()와 유사한 개념
     * - 화면 초기화 작업을 여기서 수행
     *
     * @param savedInstanceState: 이전 상태를 복원할 때 사용 (화면 회전 등)
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // 부모 클래스(AppCompatActivity)의 onCreate를 먼저 호출
        // 이는 Android 시스템이 Activity를 제대로 초기화하기 위해 필수
        super.onCreate(savedInstanceState)

        /**
         * setContentView: XML 레이아웃 파일을 이 Activity의 화면으로 설정
         * - R.layout.activity_main_home은 res/layout/activity_main_home.xml 파일을 가리킴
         * - R: Android가 자동 생성하는 리소스 접근 클래스
         */
        setContentView(R.layout.activity_main_home)

        // 뷰 초기화 (XML에서 정의한 버튼들을 코틀린 변수와 연결)
        // Initialize views (connect buttons defined in XML with Kotlin variables)
        initializeViews()

        // 버튼 클릭 리스너 설정 (버튼 클릭 시 동작 정의)
        // Set up button click listeners (define actions when buttons are clicked)
        setupClickListeners()
    }

    /**
     * XML 레이아웃에서 정의한 뷰들을 코틀린 변수와 연결하는 메서드
     * Method to connect views defined in XML layout with Kotlin variables
     *
     * findViewById<타입>(R.id.뷰ID):
     * - XML에서 android:id로 지정한 ID를 사용하여 뷰를 찾음
     * - <타입>: 제네릭 타입 지정 (여기서는 Button)
     * - iOS의 @IBOutlet과 유사한 개념
     */
    private fun initializeViews() {
        // 기능 버튼 초기화
        // Initialize function buttons
        btnSeaTemperature = findViewById(R.id.btnSeaTemperature)
        btnPoint = findViewById(R.id.btnPoint)
        btnStartTracking = findViewById(R.id.btnStartTracking)

        // 지도 선택 버튼 초기화
        // Initialize map selection buttons
        btnAppleMap = findViewById(R.id.btnAppleMap)
        btnKakaoMap = findViewById(R.id.btnKakaoMap)
    }

    /**
     * 각 버튼의 클릭 이벤트를 설정하는 메서드
     * Method to set up click events for each button
     */
    private fun setupClickListeners() {

        // ===== 기능 버튼 클릭 리스너 설정 =====
        // Set up function button click listeners

        /**
         * setOnClickListener: 버튼이 클릭됐을 때 실행할 코드를 설정
         * - { }: 람다 표현식 (익명 함수를 간단하게 표현)
         * - iOS의 클로저(closure)와 동일한 개념
         */

        // 수온정보 버튼 클릭 시
        // When Sea Temperature button is clicked
        btnSeaTemperature.setOnClickListener {
            // 아직 기능이 구현되지 않았으므로 Toast 메시지로 알림
            // Show Toast message as the feature is not implemented yet
            showToast("수온정보 화면으로 이동합니다")

            // TODO: 나중에 수온정보 화면으로 이동하는 코드 추가
            // TODO: Add code to navigate to Sea Temperature screen later
            // 예시: startActivity(Intent(this, SeaTemperatureActivity::class.java))
        }

        // 포인트 버튼 클릭 시
        // When Point button is clicked
        btnPoint.setOnClickListener {
            showToast("포인트 화면으로 이동합니다")

            // TODO: 포인트 목록 화면으로 이동
            // TODO: Navigate to Point list screen
        }

        // 추적시작 버튼 클릭 시
        // When Start Tracking button is clicked
        btnStartTracking.setOnClickListener {
            showToast("추적을 시작합니다")

            // TODO: GPS 추적 시작 및 트랙 맵 화면으로 이동
            // TODO: Start GPS tracking and navigate to Track Map screen
        }

        // ===== 지도 선택 버튼 클릭 리스너 설정 =====
        // Set up map selection button click listeners

        /**
         * 지도 타입을 변경하고 버튼 스타일을 업데이트
         * Change map type and update button styles
         *
         * 토글 방식: 하나를 선택하면 다른 하나는 자동으로 선택 해제
         * Toggle method: Selecting one automatically deselects the other
         */

        // Apple Map 버튼 클릭 시
        // When Apple Map button is clicked
        btnAppleMap.setOnClickListener {
            // 이미 선택된 상태가 아닐 때만 동작
            // Only works if not already selected
            if (selectedMapType != MapType.APPLE_MAP) {
                // 선택된 지도 타입을 Apple Map으로 변경
                // Change selected map type to Apple Map
                selectedMapType = MapType.APPLE_MAP

                // 버튼 스타일 업데이트 (선택/비선택 상태 표시)
                // Update button styles (show selected/unselected state)
                updateMapButtonStyles()

                showToast("Apple Map이 선택되었습니다")
            }
        }

        // Kakao Map 버튼 클릭 시
        // When Kakao Map button is clicked
        btnKakaoMap.setOnClickListener {
            if (selectedMapType != MapType.KAKAO_MAP) {
                selectedMapType = MapType.KAKAO_MAP
                updateMapButtonStyles()
                showToast("Kakao Map이 선택되었습니다")
            }
        }
    }

    /**
     * 지도 선택 버튼의 스타일을 업데이트하는 메서드
     * Method to update map selection button styles
     *
     * 선택된 버튼: 진한 배경, 선택되지 않은 버튼: 연한 배경
     * Selected button: darker background, Unselected button: lighter background
     */
    private fun updateMapButtonStyles() {
        /**
         * when: 코틀린의 조건문 (Java의 switch문과 유사)
         * - iOS Swift의 switch문과 동일한 개념
         * - selectedMapType의 값에 따라 다른 동작 수행
         */
        when (selectedMapType) {
            MapType.APPLE_MAP -> {
                // Apple Map이 선택된 경우
                // When Apple Map is selected

                /**
                 * setBackgroundResource: 뷰의 배경 리소스를 설정
                 * - R.drawable.button_background_selected: 선택된 버튼 배경
                 * - R.drawable.button_background: 기본 버튼 배경
                 */
                btnAppleMap.setBackgroundResource(R.drawable.button_background_selected)
                btnKakaoMap.setBackgroundResource(R.drawable.button_background)

                /**
                 * tag: 뷰에 추가 정보를 저장할 수 있는 속성
                 * - 나중에 버튼의 상태를 확인할 때 사용 가능
                 */
                btnAppleMap.tag = "selected"
                btnKakaoMap.tag = "unselected"
            }
            MapType.KAKAO_MAP -> {
                // Kakao Map이 선택된 경우
                // When Kakao Map is selected
                btnAppleMap.setBackgroundResource(R.drawable.button_background)
                btnKakaoMap.setBackgroundResource(R.drawable.button_background_selected)

                btnAppleMap.tag = "unselected"
                btnKakaoMap.tag = "selected"
            }
        }
    }

    /**
     * Toast 메시지를 표시하는 헬퍼 메서드
     * Helper method to display Toast messages
     *
     * Toast: 화면 하단에 잠깐 나타났다 사라지는 메시지
     * - iOS의 Alert이나 Notification과 비슷한 개념
     * - 사용자에게 간단한 피드백을 제공할 때 사용
     *
     * @param message: 표시할 메시지 문자열
     */
    private fun showToast(message: String) {
        /**
         * Toast.makeText(): Toast 객체를 생성
         * - this: 현재 Activity의 Context (Android에서 앱 환경 정보를 담는 객체)
         * - message: 표시할 메시지
         * - Toast.LENGTH_SHORT: 짧은 시간 동안 표시 (약 2초)
         *   Toast.LENGTH_LONG은 긴 시간 (약 3.5초)
         *
         * .show(): Toast를 실제로 화면에 표시
         */
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * 지도 타입을 나타내는 열거형 (Enum)
     * Enumeration representing map types
     *
     * enum class: 정해진 값들만 가질 수 있는 타입
     * - 타입 안정성을 제공 (잘못된 값이 들어가는 것을 방지)
     * - iOS Swift의 enum과 동일한 개념
     */
    enum class MapType {
        APPLE_MAP,   // Apple 지도
        KAKAO_MAP    // Kakao 지도
    }
}
