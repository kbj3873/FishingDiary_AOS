package com.bj.fishingdiary.presentation.track

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bj.fishingdiary.R
import com.bj.fishingdiary.data.repository.DefaultTrackMapRepository
import com.bj.fishingdiary.data.storage.FileDataStorageImpl
import com.bj.fishingdiary.domain.entity.MapType
import com.bj.fishingdiary.domain.usecase.TrackMapUseCase
import com.bj.fishingdiary.managers.FDAppManager
import com.bj.fishingdiary.managers.FDFileManager
import com.bj.fishingdiary.presentation.point.map_strategy.GoogleMapStrategy
import com.bj.fishingdiary.presentation.point.map_strategy.KakaoMapStrategy
import com.bj.fishingdiary.presentation.point.map_strategy.MapStrategy
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.launch

/**
 * 트랙 지도 화면
 * Track Map Activity
 *
 * 실시간 GPS 위치 추적 및 경로 표시
 * Point의 MapStrategy를 재사용하여 구현
 */
class TrackMapActivity : AppCompatActivity() {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    private lateinit var viewModel: TrackMapViewModel
    private lateinit var mapStrategy: MapStrategy
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // UI Views
    private lateinit var tvSpeed: TextView
    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView
    private lateinit var btnTracking: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_map)

        // FusedLocationProviderClient 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        initializeViews()
        initializeViewModel()
        initializeMap()
        observeViewModel()

        // 위치 권한 확인 및 요청
        checkAndRequestLocationPermissions()
    }

    private fun initializeViews() {
        tvSpeed = findViewById(R.id.tv_speed)
        tvLatitude = findViewById(R.id.tv_latitude)
        tvLongitude = findViewById(R.id.tv_longitude)
        btnTracking = findViewById(R.id.btn_tracking)

        btnTracking.setOnClickListener {
            if (viewModel.isTracking.value) {
                stopTracking()
            } else {
                startTracking()
            }
        }
    }

    private fun initializeViewModel() {
        val fileManager = FDFileManager.getInstance(applicationContext)
        val storage = FileDataStorageImpl(fileManager)
        val repository = DefaultTrackMapRepository(storage)
        val useCase = TrackMapUseCase(repository)

        val factory = TrackMapViewModelFactory(applicationContext, useCase)
        viewModel = ViewModelProvider(this, factory).get(TrackMapViewModel::class.java)
    }

    private fun initializeMap() {
        // FDAppManager에서 현재 선택된 지도 타입 확인
        val mapType = FDAppManager.getInstance().mapType
        val containerId = R.id.map_container

        mapStrategy = when (mapType) {
            MapType.GOOGLE_MAP -> {
                GoogleMapStrategy(supportFragmentManager, containerId)
            }
            MapType.KAKAO_MAP -> {
                val container = findViewById<ViewGroup>(containerId)
                KakaoMapStrategy(this, container)
            }
            else -> GoogleMapStrategy(supportFragmentManager, containerId) // Fallback
        }

        mapStrategy.initialize {
            // 지도 로드 완료 후 처리
            android.util.Log.d("TrackMapActivity", "Map initialized, mapStrategy type: ${mapStrategy::class.simpleName}")

            // 현재 위치 표시 활성화
            mapStrategy.setMyLocationEnabled(true)
            android.util.Log.d("TrackMapActivity", "setMyLocationEnabled(true) called")

            // 현재 위치로 카메라 이동
            moveToCurrentLocation()
        }
    }

    private fun observeViewModel() {
        // 속도 업데이트
        lifecycleScope.launch {
            viewModel.currentSpeed.collect { speed ->
                tvSpeed.text = "$speed km/h"
            }
        }

        // 위도 업데이트
        lifecycleScope.launch {
            viewModel.currentLatitude.collect { latitude ->
                tvLatitude.text = latitude
            }
        }

        // 경도 업데이트
        lifecycleScope.launch {
            viewModel.currentLongitude.collect { longitude ->
                tvLongitude.text = longitude
            }
        }

        // 추적 상태 업데이트
        lifecycleScope.launch {
            viewModel.isTracking.collect { isTracking ->
                btnTracking.text = if (isTracking) "추적 중지" else "추적 시작"
            }
        }

        // 실시간 경로 라인 추가
        lifecycleScope.launch {
            viewModel.trackLineSegment.collect { segment ->
                segment?.let { (prevLatLng, currLatLng) ->
                    // 이전 위치 -> 현재 위치 라인 그리기
                    val points = listOf(prevLatLng, currLatLng)

                    // 속도에 따른 색상 결정
                    val speed = viewModel.currentSpeed.value.toFloatOrNull() ?: 0f
                    val color = if (speed < 5.0f) 0xFFFF0000.toInt() else 0xFF0000FF.toInt()

                    mapStrategy.addPolyline(points, color, 10f)
                }
            }
        }

        // 현재 위치 업데이트 (카메라 이동 및 마커 업데이트)
        lifecycleScope.launch {
            viewModel.currentLocation.collect { location ->
                location?.let {
                    android.util.Log.d("TrackMapActivity", "Current location updated: ${it.latitude}, ${it.longitude}")

                    // Kakao Map인 경우 현재 위치 마커 업데이트
                    if (mapStrategy is KakaoMapStrategy) {
                        android.util.Log.d("TrackMapActivity", "Calling updateMyLocation for KakaoMapStrategy")
                        (mapStrategy as KakaoMapStrategy).updateMyLocation(it)
                    } else {
                        android.util.Log.d("TrackMapActivity", "Using Google Maps built-in location")
                    }

                    // 카메라를 현재 위치로 이동 (zoom level 17)
                    val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
                        .include(it)
                        .build()
                    mapStrategy.moveCamera(bounds, 0)
                }
            }
        }
    }

    private fun startTracking() {
        android.util.Log.d("Location Debug", "TrackMapActivity.startTracking called")
        // 위치 권한 확인
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 권한이 없으면 요청
            android.widget.Toast.makeText(
                this,
                "위치 권한이 필요합니다.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            checkAndRequestLocationPermissions()
            return
        }

        viewModel.startTracking()
        android.util.Log.d("TrackMapActivity", "Tracking started")
    }

    private fun stopTracking() {
        viewModel.stopTracking()
        android.util.Log.d("TrackMapActivity", "Tracking stopped")
    }

    /**
     * 위치 권한 확인 및 요청
     */
    private fun checkAndRequestLocationPermissions() {
        val fineLocationGranted = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocationGranted || !coarseLocationGranted) {
            // 권한이 없으면 요청
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // 권한이 이미 있으면 현재 위치로 이동
            android.util.Log.d("TrackMapActivity", "Location permissions already granted")
        }
    }

    /**
     * 권한 요청 결과 처리
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 권한이 승인되면 현재 위치로 이동
                    android.util.Log.d("TrackMapActivity", "Location permission granted")
                    moveToCurrentLocation()
                } else {
                    // 권한이 거부되면 메시지 표시
                    android.util.Log.w("TrackMapActivity", "Location permission denied")
                    android.widget.Toast.makeText(
                        this,
                        "위치 권한이 필요합니다. 설정에서 권한을 허용해주세요.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * 현재 위치로 카메라 이동
     */
    private fun moveToCurrentLocation() {
        // 위치 권한 확인
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            android.util.Log.w("TrackMapActivity", "Location permission not granted")
            return
        }

        // 마지막으로 알려진 위치 가져오기
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = com.google.android.gms.maps.model.LatLng(
                    location.latitude,
                    location.longitude
                )

                // 카메라를 현재 위치로 이동 (zoom level 15)
                mapStrategy.moveCameraToPosition(currentLatLng, 15f)
                
                // Kakao Map인 경우 현재 위치 마커 즉시 업데이트
                if (mapStrategy is KakaoMapStrategy) {
                    (mapStrategy as KakaoMapStrategy).updateMyLocation(currentLatLng)
                }

                android.util.Log.d("TrackMapActivity", "Camera moved to current location: ${location.latitude}, ${location.longitude}, zoom=15")
            } else {
                android.util.Log.w("TrackMapActivity", "Last known location is null")
            }
        }.addOnFailureListener { exception ->
            android.util.Log.e("TrackMapActivity", "Failed to get location", exception)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // ViewModel이 자동으로 정리됨 (onCleared 호출)
    }
}
