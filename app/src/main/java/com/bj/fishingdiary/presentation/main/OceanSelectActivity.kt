package com.bj.fishingdiary.presentation.main

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bj.fishingdiary.R
import com.bj.fishingdiary.application.di.DataServiceDIContainer
import kotlinx.coroutines.launch

/**
 * 수온 즐겨찾기 화면
 * Ocean Favorites Screen
 *
 * iOS의 OceanSelectViewController에 대응
 *
 * 역할:
 * - 전체 수온 관측소 목록 표시
 * - 사용자가 즐겨찾기할 관측소 선택
 * - 선택된 관측소는 SharedPreferences에 저장
 * - 메인 화면에서 선택된 관측소만 표시
 *
 * AppCompatActivity란?
 * - Android의 기본 Activity를 확장한 클래스
 * - 하위 버전 Android에서도 최신 기능 사용 가능
 * - iOS의 UIViewController와 유사한 역할
 */
class OceanSelectActivity : AppCompatActivity() {

    // ==================== UI Components ====================

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnClose: Button

    // ==================== Dependencies ====================

    private lateinit var viewModel: OceanSelectViewModel
    private lateinit var adapter: OceanSelectAdapter

    // ==================== Lifecycle ====================

    /**
     * Activity 생성 시 호출
     * Called when Activity is created
     *
     * iOS의 viewDidLoad()와 유사
     *
     * @param savedInstanceState 이전 상태 저장 데이터 (화면 회전 시 등)
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ocean_select)

        // DI Container에서 ViewModel 생성
        val dataServiceDIContainer = DataServiceDIContainer()
        val mainSceneDIContainer = dataServiceDIContainer.makeMainSceneDIContainer()
        viewModel = mainSceneDIContainer.makeOceanSelectViewModel(applicationContext)

        // UI 초기화
        setupViews()
        setupRecyclerView()
        setupListeners()
        bindViewModel()

        // 데이터 로드
        viewModel.viewDidLoad()
    }

    /**
     * Activity 종료 시 호출
     * Called when Activity is destroyed
     */
    override fun onDestroy() {
        super.onDestroy()
        viewModel.onCleared()
    }

    // ==================== Setup Methods ====================

    /**
     * 뷰 초기화
     * Initialize views
     *
     * findViewById로 XML 레이아웃의 뷰들을 참조
     * iOS의 @IBOutlet과 유사
     */
    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerViewOceanSelect)
        btnClose = findViewById(R.id.btnClose)
    }

    /**
     * RecyclerView 설정
     * Setup RecyclerView
     *
     * iOS의 tableView 설정과 유사
     */
    private fun setupRecyclerView() {
        // Adapter 생성 (아이템 클릭 리스너 전달)
        adapter = OceanSelectAdapter { model ->
            // 체크 상태 토글
            viewModel.saveCheckList(!model.isChecked, model)
        }

        // RecyclerView 설정
        recyclerView.apply {
            this.adapter = this@OceanSelectActivity.adapter
            layoutManager = LinearLayoutManager(this@OceanSelectActivity)
        }
    }

    /**
     * 리스너 설정
     * Setup listeners
     *
     * 버튼 클릭 이벤트 처리
     */
    private fun setupListeners() {
        // 닫기 버튼 클릭 시 Activity 종료
        btnClose.setOnClickListener {
            finish()  // iOS의 dismiss()와 유사
        }
    }

    /**
     * ViewModel과 바인딩
     * Bind with ViewModel
     *
     * StateFlow를 관찰하여 UI 업데이트
     * iOS의 Combine sink(receiveValue:)와 유사
     */
    private fun bindViewModel() {
        // items StateFlow 관찰
        lifecycleScope.launch {
            viewModel.items.collect { stations ->
                // 데이터 변경 시 RecyclerView 업데이트
                adapter.submitList(stations)
            }
        }

        // 로딩 상태 관찰 (필요시 ProgressBar 표시)
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                // TODO: 로딩 인디케이터 표시/숨김
            }
        }

        // 에러 메시지 관찰 (필요시 Toast 표시)
        lifecycleScope.launch {
            viewModel.errorMessage.collect { errorMessage ->
                errorMessage?.let {
                    // TODO: 에러 메시지 표시
                    println("Error: $it")
                }
            }
        }
    }
}

/**
 * Activity 사용 예시:
 * Activity Usage Example:
 *
 * ```kotlin
 * // 메인 화면에서 OceanSelectActivity 실행
 * val intent = Intent(this, OceanSelectActivity::class.java)
 * startActivity(intent)
 * ```
 */
