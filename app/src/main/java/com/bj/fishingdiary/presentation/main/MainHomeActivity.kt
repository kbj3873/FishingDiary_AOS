package com.bj.fishingdiary.presentation.main

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bj.fishingdiary.R
import com.bj.fishingdiary.application.di.DataServiceDIContainer
import com.bj.fishingdiary.domain.entity.MapType
import kotlinx.coroutines.launch

/**
 * 메인 홈 화면 Activity
 * Main Home Screen Activity
 *
 * Activity란?
 * - Android에서 화면 하나를 나타내는 기본 단위
 * - iOS의 UIViewController 또는 SwiftUI의 View와 유사한 개념
 * - AppCompatActivity를 상속받아 하위 버전 Android와의 호환성 제공
 *
 * 주요 기능:
 * - 수온 정보 리스트 표시
 * - Pull to Refresh (아래로 당겨서 새로고침)
 * - 수온 즐겨찾기 버튼
 * - 지도 선택 (Apple Map / Kakao Map)
 * - 기능 버튼 (수온정보, 포인트, 추적시작)
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

    // 수온 정보 리스트 관련 뷰
    // Temperature information list related views
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout  // Pull to Refresh
    private lateinit var recyclerViewTemperature: RecyclerView   // 수온 리스트
    private lateinit var btnOceanFavorites: Button               // 즐겨찾기 버튼

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
     */
    private var selectedMapType: MapType = MapType.APPLE_MAP

    // ===== ViewModel 및 Adapter =====
    // ViewModel and Adapter

    /**
     * MainViewModel 인스턴스
     * - 수온 정보 데이터 관리
     * - API 호출 및 상태 관리
     */
    private lateinit var viewModel: MainViewModel

    /**
     * RecyclerView Adapter 인스턴스
     * - 수온 정보 리스트 표시
     */
    private val temperatureAdapter = TemperatureAdapter()

    // ===== Activity 생명주기 메서드 =====
    // Activity Lifecycle Methods

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

        // DIContainer를 사용하여 ViewModel 생성
        // Create ViewModel using DIContainer
        val dataServiceDIContainer = DataServiceDIContainer()
        val mainSceneDIContainer = dataServiceDIContainer.makeMainSceneDIContainer()
        viewModel = mainSceneDIContainer.makeMainViewModel(applicationContext)

        // 뷰 초기화 (XML에서 정의한 뷰들을 코틀린 변수와 연결)
        // Initialize views (connect views defined in XML with Kotlin variables)
        initializeViews()

        // RecyclerView 설정
        // Set up RecyclerView
        setupRecyclerView()

        // Pull to Refresh 설정
        // Set up Pull to Refresh
        setupSwipeRefresh()

        // 버튼 클릭 리스너 설정
        // Set up button click listeners
        setupClickListeners()

        // ViewModel 상태 구독
        // Subscribe to ViewModel states
        observeViewModel()

        // 초기 데이터 로드
        // Load initial data
        viewModel.viewDidLoad()
    }

    // ===== 초기화 메서드 =====
    // Initialization Methods

    /**
     * XML 레이아웃에서 정의한 뷰들을 코틀린 변수와 연결하는 메서드
     * Method to connect views defined in XML layout with Kotlin variables
     */
    private fun initializeViews() {
        // 수온 정보 리스트 관련 뷰
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        recyclerViewTemperature = findViewById(R.id.recyclerViewTemperature)
        btnOceanFavorites = findViewById(R.id.btnOceanFavorites)

        // 기능 버튼 초기화
        btnSeaTemperature = findViewById(R.id.btnSeaTemperature)
        btnPoint = findViewById(R.id.btnPoint)
        btnStartTracking = findViewById(R.id.btnStartTracking)

        // 지도 선택 버튼 초기화
        btnAppleMap = findViewById(R.id.btnAppleMap)
        btnKakaoMap = findViewById(R.id.btnKakaoMap)
    }

    /**
     * RecyclerView 설정
     * Set up RecyclerView
     *
     * RecyclerView란?
     * - 스크롤 가능한 리스트를 효율적으로 표시하는 뷰
     * - iOS의 UITableView와 유사
     */
    private fun setupRecyclerView() {
        recyclerViewTemperature.apply {
            // Adapter 설정
            adapter = temperatureAdapter

            // LayoutManager 설정
            // LinearLayoutManager: 세로 방향 리스트 (iOS UITableView와 유사)
            layoutManager = LinearLayoutManager(this@MainHomeActivity)

            // 아이템 크기가 고정되어 있으면 true로 설정하여 성능 향상
            setHasFixedSize(true)
        }
    }

    /**
     * Pull to Refresh 설정
     * Set up Pull to Refresh (Swipe to Refresh)
     *
     * iOS의 UIRefreshControl과 동일한 기능
     */
    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            // 사용자가 아래로 당겨서 새로고침할 때 호출
            viewModel.fetchRisaList()
        }
    }

    /**
     * 각 버튼의 클릭 이벤트를 설정하는 메서드
     * Method to set up click events for each button
     */
    private fun setupClickListeners() {

        // ===== 수온 즐겨찾기 버튼 =====
        // Ocean Favorites Button
        btnOceanFavorites.setOnClickListener {
            // TODO: 즐겨찾기 관리 화면으로 이동
            showToast("수온 즐겨찾기 화면으로 이동합니다")
        }

        // ===== 기능 버튼 클릭 리스너 설정 =====
        // Set up function button click listeners

        // 수온정보 버튼 클릭 시
        btnSeaTemperature.setOnClickListener {
            showToast("수온정보 화면으로 이동합니다")
            // TODO: 수온정보 화면으로 이동
        }

        // 포인트 버튼 클릭 시
        btnPoint.setOnClickListener {
            showToast("포인트 화면으로 이동합니다")
            // TODO: 포인트 목록 화면으로 이동
        }

        // 추적시작 버튼 클릭 시
        btnStartTracking.setOnClickListener {
            showToast("추적을 시작합니다")
            // TODO: GPS 추적 시작 및 트랙 맵 화면으로 이동
        }

        // ===== 지도 선택 버튼 클릭 리스너 설정 =====
        // Set up map selection button click listeners

        // Apple Map 버튼 클릭 시
        btnAppleMap.setOnClickListener {
            if (selectedMapType != MapType.APPLE_MAP) {
                selectedMapType = MapType.APPLE_MAP
                updateMapButtonStyles()
                showToast("Apple Map이 선택되었습니다")
            }
        }

        // Kakao Map 버튼 클릭 시
        btnKakaoMap.setOnClickListener {
            if (selectedMapType != MapType.KAKAO_MAP) {
                selectedMapType = MapType.KAKAO_MAP
                updateMapButtonStyles()
                showToast("Kakao Map이 선택되었습니다")
            }
        }
    }

    /**
     * ViewModel의 상태를 구독하는 메서드
     * Method to observe ViewModel states
     *
     * StateFlow 구독:
     * - lifecycleScope: Activity 생명주기에 맞춰 자동으로 구독 시작/종료
     * - iOS의 Combine sink와 유사한 개념
     */
    private fun observeViewModel() {
        // 수온 정보 리스트 구독
        lifecycleScope.launch {
            viewModel.oceanStations.collect { stations ->
                // 데이터가 변경될 때마다 호출
                temperatureAdapter.submitList(stations)
            }
        }

        // 로딩 상태 구독
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                // 로딩 상태에 따라 SwipeRefreshLayout 업데이트
                swipeRefreshLayout.isRefreshing = isLoading
            }
        }

        // 에러 메시지 구독
        lifecycleScope.launch {
            viewModel.errorMessage.collect { errorMessage ->
                errorMessage?.let {
                    showToast("에러: $it")
                }
            }
        }
    }

    // ===== UI 업데이트 메서드 =====
    // UI Update Methods

    /**
     * 지도 선택 버튼의 스타일을 업데이트하는 메서드
     * Method to update map selection button styles
     */
    private fun updateMapButtonStyles() {
        when (selectedMapType) {
            MapType.APPLE_MAP -> {
                btnAppleMap.setBackgroundResource(R.drawable.button_background_selected)
                btnKakaoMap.setBackgroundResource(R.drawable.button_background)
                btnAppleMap.tag = "selected"
                btnKakaoMap.tag = "unselected"
            }
            MapType.KAKAO_MAP -> {
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
     */
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // ===== Activity 생명주기 정리 =====
    // Activity Lifecycle Cleanup

    /**
     * onDestroy: Activity가 제거될 때 호출
     * Called when Activity is destroyed
     *
     * 리소스 정리 및 작업 취소
     */
    override fun onDestroy() {
        super.onDestroy()
        // ViewModel 정리
        viewModel.onCleared()
    }
}
