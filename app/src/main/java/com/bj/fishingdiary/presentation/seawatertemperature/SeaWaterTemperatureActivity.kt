package com.bj.fishingdiary.presentation.seawatertemperature

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.bj.fishingdiary.R
import com.bj.fishingdiary.application.di.DataServiceDIContainer
import com.bj.fishingdiary.common.extensions.subStrings
import com.bj.fishingdiary.domain.entity.*
import kotlinx.coroutines.launch

/**
 * 수온 정보 화면 Activity
 * Sea Water Temperature Screen Activity
 *
 * iOS의 SeaWaterTemperatureViewController에 대응
 * Corresponds to SeaWaterTemperatureViewController in iOS
 *
 * 주요 기능:
 * Main features:
 * - 해역 및 관측소 선택
 * - Select sea area and observation station
 * - 표층/중층/저층 수온 그래프 표시
 * - Display surface/middle/bottom temperature graphs
 */
class SeaWaterTemperatureActivity : AppCompatActivity() {

    // ==================== View ====================

    private lateinit var toolbar: Toolbar
    private lateinit var seaSpinner: Spinner
    private lateinit var observSpinner: Spinner
    private lateinit var searchButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var graphContainer: LinearLayout

    // Title Labels
    private lateinit var surTitleLabel: TextView
    private lateinit var midTitleLabel: TextView
    private lateinit var botTitleLabel: TextView

    // Temperature Labels
    private lateinit var surTempLabel: TextView
    private lateinit var midTempLabel: TextView
    private lateinit var botTempLabel: TextView

    // Graph Views
    private lateinit var surGraphView: TemperatureLineGraphView
    private lateinit var midGraphView: TemperatureLineGraphView
    private lateinit var botGraphView: TemperatureLineGraphView

    // Date Label Containers
    private lateinit var surDateLabelContainer: LinearLayout
    private lateinit var midDateLabelContainer: LinearLayout
    private lateinit var botDateLabelContainer: LinearLayout

    // ==================== ViewModel ====================

    private lateinit var viewModel: SeaWaterTemperatureViewModel

    // ==================== Data ====================

    private val seaList = Sea.values().toList()
    private var currentObservList: List<Observ> = emptyList()

    private var selectedSea: Sea = Sea.NONE
    private var selectedObserv: Observ? = null

    // ==================== Lifecycle ====================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sea_water_temperature)

        // Initialize ViewModel
        val diContainer = DataServiceDIContainer()
        val mainSceneDIContainer = diContainer.makeMainSceneDIContainer()
        viewModel = mainSceneDIContainer.makeSeaWaterTemperatureViewModel()

        // Initialize Views
        initViews()

        // Setup Toolbar
        setupToolbar()

        // Setup Spinners
        setupSpinners()

        // Setup Button
        setupButton()

        // Observe ViewModel
        observeViewModel()
    }

    // ==================== Initialize ====================

    /**
     * 뷰 초기화
     * Initialize views
     */
    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        seaSpinner = findViewById(R.id.seaSpinner)
        observSpinner = findViewById(R.id.observSpinner)
        searchButton = findViewById(R.id.searchButton)
        progressBar = findViewById(R.id.progressBar)
        graphContainer = findViewById(R.id.graphContainer)

        surTitleLabel = findViewById(R.id.surTitleLabel)
        midTitleLabel = findViewById(R.id.midTitleLabel)
        botTitleLabel = findViewById(R.id.botTitleLabel)

        surTempLabel = findViewById(R.id.surTempLabel)
        midTempLabel = findViewById(R.id.midTempLabel)
        botTempLabel = findViewById(R.id.botTempLabel)

        surGraphView = findViewById(R.id.surGraphView)
        midGraphView = findViewById(R.id.midGraphView)
        botGraphView = findViewById(R.id.botGraphView)

        surDateLabelContainer = findViewById(R.id.surDateLabelContainer)
        midDateLabelContainer = findViewById(R.id.midDateLabelContainer)
        botDateLabelContainer = findViewById(R.id.botDateLabelContainer)
    }

    /**
     * Toolbar 설정
     * Setup toolbar
     */
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    /**
     * Spinner 설정
     * Setup spinners
     */
    private fun setupSpinners() {
        // 해역 Spinner
        // Sea area spinner
        val seaAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            seaList.map { it.displayName }
        )
        seaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        seaSpinner.adapter = seaAdapter

        seaSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedSea = seaList[position]
                viewModel.selectSea(selectedSea)
                updateObservSpinner()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 관측소 Spinner
        // Observation station spinner
        updateObservSpinner()
    }

    /**
     * 관측소 Spinner 업데이트
     * Update observation station spinner
     */
    private fun updateObservSpinner() {
        currentObservList = when (selectedSea) {
            Sea.NONE -> emptyList()
            Sea.WEST -> WestObserv.values().toList()
            Sea.EAST -> EastObserv.values().toList()
            Sea.SOUTH -> SouthObserv.values().toList()
        }

        val observAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            currentObservList.map { it.title }
        )
        observAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        observSpinner.adapter = observAdapter

        observSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (currentObservList.isNotEmpty()) {
                    selectedObserv = currentObservList[position]
                    viewModel.selectObserv(selectedObserv)
                    updateSearchButtonState()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 초기화 시 첫 번째 항목 선택
        // Select first item on initialization
        if (currentObservList.isNotEmpty()) {
            selectedObserv = currentObservList[0]
            viewModel.selectObserv(selectedObserv)
        } else {
            selectedObserv = null
            viewModel.selectObserv(null)
        }

        updateSearchButtonState()
    }

    /**
     * 조회 버튼 상태 업데이트
     * Update search button state
     */
    private fun updateSearchButtonState() {
        val isEnabled = selectedSea != Sea.NONE &&
                selectedObserv != null &&
                selectedObserv?.cd?.isNotEmpty() == true
        searchButton.isEnabled = isEnabled
    }

    /**
     * 버튼 설정
     * Setup button
     */
    private fun setupButton() {
        searchButton.setOnClickListener {
            val sea = selectedSea
            val observ = selectedObserv

            if (sea != Sea.NONE && observ != null && observ.cd.isNotEmpty()) {
                clearViews()
                viewModel.fetchTemperatureData(sea.id, observ.cd)
            }
        }
    }

    /**
     * ViewModel 관찰
     * Observe ViewModel
     */
    private fun observeViewModel() {
        // 로딩 상태 관찰
        // Observe loading state
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        // 수온 데이터 관찰
        // Observe temperature data
        lifecycleScope.launch {
            viewModel.temperatureItems.collect { items ->
                if (items.isNotEmpty()) {
                    updateUI(items)
                }
            }
        }
    }

    /**
     * UI 업데이트
     * Update UI
     */
    private fun updateUI(items: List<SeaWaterTemperatureSet>) {
        graphContainer.visibility = View.VISIBLE

        // Title Labels 설정
        // Set title labels
        setTitleLabel(surTitleLabel, "표층수온", items.map { it.surDep })
        setTitleLabel(midTitleLabel, "중층수온", items.map { it.midDep })
        setTitleLabel(botTitleLabel, "저층수온", items.map { it.botDep })

        // Temperature Labels 설정
        // Set temperature labels
        setCurrentTempLabel(surTempLabel, items.map { it.surTemp })
        setCurrentTempLabel(midTempLabel, items.map { it.midTemp })
        setCurrentTempLabel(botTempLabel, items.map { it.botTemp })

        // 그래프 설정
        // Set graphs
        surGraphView.setData(items.map { it.surTemp }, Color.BLUE)
        midGraphView.setData(items.map { it.midTemp }, Color.GREEN)
        botGraphView.setData(items.map { it.botTemp }, Color.rgb(128, 0, 128)) // Purple

        // 날짜 레이블 설정
        // Set date labels
        setDateLabels(surDateLabelContainer, items.map { it.dateTime.toString() })
        setDateLabels(midDateLabelContainer, items.map { it.dateTime.toString() })
        setDateLabels(botDateLabelContainer, items.map { it.dateTime.toString() })
    }

    /**
     * Title Label 설정
     * Set title label
     */
    private fun setTitleLabel(label: TextView, title: String, depList: List<Float>) {
        var dep = -1f
        for (m in depList) {
            if (m > 0) {
                dep = m
                break
            }
        }

        label.text = if (dep < 0) {
            title
        } else {
            String.format("%s (수심 %.1fm)", title, dep)
        }
    }

    /**
     * 현재 수온 Label 설정
     * Set current temperature label
     */
    private fun setCurrentTempLabel(label: TextView, tempList: List<Float>) {
        val list = tempList.filter { it > 0 }
        if (list.isNotEmpty()) {
            val temp = list.last()
            label.text = String.format("수온 %.1f°C", temp)
        } else {
            label.text = ""
        }
    }

    /**
     * 날짜 레이블 설정
     * Set date labels
     */
    private fun setDateLabels(container: LinearLayout, dateList: List<String>) {
        container.removeAllViews()

        // 자정 시간만 필터링
        // Filter midnight times only
        val dayTimes = dateList.filter { it.endsWith("0000") }

        if (dayTimes.isEmpty()) return

        for (day in dayTimes) {
            if (day.length < 8) continue

            val month = day.subStrings(4, 5).toIntOrNull() ?: continue
            val dayNum = day.subStrings(6, 7).toIntOrNull() ?: continue

            val labelText = "$month/$dayNum"

            val textView = TextView(this).apply {
                text = labelText
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                gravity = android.view.Gravity.CENTER
            }

            container.addView(textView)
        }
    }

    /**
     * 뷰 초기화
     * Clear views
     */
    private fun clearViews() {
        surDateLabelContainer.removeAllViews()
        midDateLabelContainer.removeAllViews()
        botDateLabelContainer.removeAllViews()

        viewModel.clearData()
        graphContainer.visibility = View.GONE
    }
}
