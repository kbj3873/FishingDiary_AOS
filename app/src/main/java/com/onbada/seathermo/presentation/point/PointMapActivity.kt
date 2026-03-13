package com.onbada.seathermo.presentation.point

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.onbada.seathermo.R
import com.onbada.seathermo.common.utils.MapUtils
import com.onbada.seathermo.data.repository.DefaultPointMapRepository
import com.onbada.seathermo.data.storage.FileDataStorageImpl
import com.onbada.seathermo.domain.entity.DMSType
import com.onbada.seathermo.domain.entity.MapPin
import com.onbada.seathermo.domain.entity.MapType
import com.onbada.seathermo.domain.entity.PointData
import com.onbada.seathermo.domain.usecase.PointMapUseCase
import com.onbada.seathermo.managers.FDAppManager
import com.onbada.seathermo.managers.FDFileManager
import com.onbada.seathermo.presentation.point.map_strategy.GoogleMapStrategy
import com.onbada.seathermo.presentation.point.map_strategy.KakaoMapStrategy
import com.onbada.seathermo.presentation.point.map_strategy.MapStrategy
import kotlinx.coroutines.launch

/**
 * 포인트 지도 화면
 * Point Map Activity
 *
 * 전략 패턴을 사용하여 Google Maps / Kakao Maps 전환 지원
 */
class PointMapActivity : AppCompatActivity() {

    private lateinit var viewModel: PointMapViewModel
    private lateinit var mapStrategy: MapStrategy

    // UI Views
    private lateinit var infoView: CardView
    private lateinit var tvTime: TextView
    private lateinit var tvSequence: TextView
    private lateinit var tvLat: TextView
    private lateinit var tvLon: TextView
    private lateinit var tvKmh: TextView
    private lateinit var tvKnot: TextView
    private lateinit var btnConvert: Button

    private var selectedMapPin: MapPin? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_point_map)

        initializeViews()
        initializeViewModel()
        initializeMap()
        setupClickListeners()
    }

    private fun initializeViews() {
        infoView = findViewById(R.id.info_view)
        tvTime = findViewById(R.id.tv_time)
        tvSequence = findViewById(R.id.tv_sequence)
        tvLat = findViewById(R.id.tv_lat)
        tvLon = findViewById(R.id.tv_lon)
        tvKmh = findViewById(R.id.tv_kmh)
        tvKnot = findViewById(R.id.tv_knot)
        btnConvert = findViewById(R.id.btn_convert)
    }

    private fun initializeViewModel() {
        // Intent로부터 데이터 수신
        val path = intent.getStringExtra("POINT_DATA_PATH") ?: ""
        val name = intent.getStringExtra("POINT_DATA_NAME") ?: "Unknown"
        val pointData = PointData(name, java.io.File(path))
        
        val fileManager = FDFileManager.getInstance(applicationContext)
        val storage = FileDataStorageImpl(fileManager)
        val repository = DefaultPointMapRepository(storage)
        val useCase = PointMapUseCase(repository)

        val factory = PointMapViewModelFactory(pointData, useCase)
        viewModel = androidx.lifecycle.ViewModelProvider(this, factory).get(PointMapViewModel::class.java)
    }

    private fun initializeMap() {
        // FDAppManager에서 현재 선택된 지도 타입 확인
        val mapType = FDAppManager.getInstance().mapType
        val containerId = R.id.map_container
        
        mapStrategy = when (mapType) {
            MapType.GOOGLE_MAP -> {
                GoogleMapStrategy(supportFragmentManager, containerId).apply {
                    onMapEmptyClick = { hideInfoView() }
                }
            }
            MapType.KAKAO_MAP -> {
                val container = findViewById<ViewGroup>(containerId)
                KakaoMapStrategy(this, container).apply {
                    onMapEmptyClick = { hideInfoView() }
                }
            }
            else -> GoogleMapStrategy(supportFragmentManager, containerId) // Fallback
        }

        mapStrategy.initialize {
            // 지도 로드 완료 시 ViewModel 관찰 시작
            observeViewModel()
        }
    }

    private fun setupClickListeners() {
        btnConvert.setOnClickListener {
            selectedMapPin?.let { pin ->
                val nextType = pin.dmsType.next()
                pin.dmsType = nextType
                updateInfoView(pin)
            }
        }
    }

    private fun observeViewModel() {
        // 카메라 이동
        lifecycleScope.launch {
            viewModel.cameraBounds.collect { bounds ->
                bounds?.let {
                    mapStrategy.moveCamera(it, 100)
                }
            }
        }

        // 경로 그리기 (Polyline)
        lifecycleScope.launch {
            viewModel.polyLineSegments.collect { segments ->
                android.util.Log.d("Kakao Map Debug", "Polyline segments received: ${segments.size}")
                mapStrategy.clear() // 그리기 전 초기화 (중복 방지)
                segments.forEach { (points, isSlow) ->
                    val color = if (isSlow) 0xFFFF0000.toInt() else 0xFF0000FF.toInt()
                    android.util.Log.d("Kakao Map Debug", "Adding Polyline: points=${points.size}, isSlow=$isSlow, color=$color")
                    mapStrategy.addPolyline(points, color, 10f)
                }
            }
        }

        // 마커 그리기 (Pin)
        lifecycleScope.launch {
            viewModel.mapPins.collect { pins ->
                android.util.Log.d("Kakao Map Debug", "Map pins received: ${pins.size}")
                pins.forEach { pin ->
                    android.util.Log.d("Kakao Map Debug", "Adding Marker: ${pin.title} at (${pin.latitude}, ${pin.longitude})")
                    mapStrategy.addMarker(pin) { clickedPin ->
                        showInfoView(clickedPin)
                    }
                }
            }
        }
    }

    private fun showInfoView(pin: MapPin) {
        selectedMapPin = pin
        updateInfoView(pin)
        infoView.visibility = View.VISIBLE
    }

    private fun hideInfoView() {
        infoView.visibility = View.GONE
        selectedMapPin = null
    }

    private fun updateInfoView(pin: MapPin) {
        val loc = pin.locationData
        
        tvTime.text = "time: ${loc.time}"
        tvSequence.text = "sequence: ${loc.sequence}"
        tvKmh.text = "${loc.kmh} km/h"
        tvKnot.text = "${loc.knot} knot"
        btnConvert.text = pin.dmsType.description

        when (pin.dmsType) {
            is DMSType.D -> {
                tvLat.text = "lat: ${loc.latitude}"
                tvLon.text = "lon: ${loc.longitude}"
            }
            is DMSType.DM -> {
                tvLat.text = "lat: ${MapUtils.convertDtoDM(pin.latitude)}"
                tvLon.text = "lon: ${MapUtils.convertDtoDM(pin.longitude)}"
            }
            is DMSType.DMS -> {
                tvLat.text = "lat: ${MapUtils.convertDtoDMS(pin.latitude)}"
                tvLon.text = "lon: ${MapUtils.convertDtoDMS(pin.longitude)}"
            }
        }
    }
}
