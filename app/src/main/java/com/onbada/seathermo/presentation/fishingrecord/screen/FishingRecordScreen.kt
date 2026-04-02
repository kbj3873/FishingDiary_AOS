package com.onbada.seathermo.presentation.fishingrecord.screen

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.onbada.seathermo.presentation.common.components.CommonPopupOverlay
import com.onbada.seathermo.presentation.common.components.PopupLayoutType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng as KakaoLatLng
import com.kakao.vectormap.camera.CameraUpdateFactory as KakaoCameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelLayer
import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.shape.MapPoints
import com.kakao.vectormap.shape.PolylineOptions as KakaoPolylineOptions
import com.kakao.vectormap.shape.PolylineStyle
import com.kakao.vectormap.shape.ShapeLayer
import com.kakao.vectormap.shape.ShapeLayerOptions
import com.onbada.seathermo.R
import com.onbada.seathermo.domain.entity.MapLineInfo
import com.onbada.seathermo.domain.entity.MapType
import com.onbada.seathermo.managers.FDAppManager
import com.onbada.seathermo.presentation.fishingrecord.screen.components.GoogleRecordMapView
import com.onbada.seathermo.presentation.fishingrecord.screen.components.KakaoRecordMapView
import com.onbada.seathermo.presentation.fishingrecord.viewmodel.FishingRecordUiState
import com.onbada.seathermo.presentation.fishingrecord.viewmodel.FishingRecordViewModel
import com.onbada.seathermo.presentation.fishingrecord.viewmodel.SpeedUnit
import com.onbada.seathermo.utility.LocationPermissionHelper
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

// ── 색상 정의 (Figma 기준) ──────────────────────────────────────────────────
// 이동 중: 파랑 #2563EB
private val StateMovingColor = Color(0xFF2563EB)
// 탐색 중: 주황 #F59E0B
private val StateDriftingColor = Color(0xFFF59E0B)
// 낚시 중: 빨강 #EF4444
private val StateFishingColor = Color(0xFFEF4444)
// 대기 중: 회색-파랑 #CAD5E2
private val StateIdleColor = Color(0xFFCAD5E2)
// 대기 중 텍스트: Slate-500 #64748B
private val StateIdleTextColor = Color(0xFF64748B)

/**
 * 낚시 기록 화면.
 *
 * [개념] iOS의 FishingRecordView.swift에 대응합니다.
 *        지도를 배경으로 깔고, 그 위에 오버레이 UI를 겹치는 구조입니다.
 *        Box는 iOS의 ZStack과 동일한 역할을 합니다.
 */
@Composable
fun FishingRecordScreen(
    viewModel: FishingRecordViewModel,
    isVisible: Boolean = true
) {
    // [개념] collectAsStateWithLifecycle()은 앱이 백그라운드 상태일 때 Flow 수집을 자동 중단합니다.
    //        Swift의 @ObservedObject와 동일하게 UI가 자동으로 상태를 반영합니다.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 현재 지도 타입을 실시간으로 구독합니다.
    // [개념] FDAppManager.mapTypeFlow는 StateFlow이므로 collectAsStateWithLifecycle()로
    //        변경 시 Recomposition이 발생합니다. 설정 탭에서 지도 타입을 변경하면
    //        이 화면도 자동으로 다시 그려집니다.
    val currentMapType by FDAppManager.getInstance().mapTypeFlow.collectAsStateWithLifecycle()

    // GoogleMap 인스턴스 보관 (지도 컨트롤 버튼에서 사용)
    // [개념] var googleMap by remember { mutableStateOf<GoogleMap?>(null) }은
    //        지도가 준비되면 참조를 저장합니다. null인 동안 컨트롤 버튼 클릭은 무시됩니다.
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }

    // KakaoMap 인스턴스 보관 (KAKAO_MAP 타입 선택 시 사용)
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    // Kakao 레이어 참조 — onMapReady에서 초기화 후 LaunchedEffect에서 사용합니다.
    var kakaoPolyLayer by remember { mutableStateOf<ShapeLayer?>(null) }
    var kakaoStateLabelLayer by remember { mutableStateOf<LabelLayer?>(null) }
    var kakaoPhotoLabelLayer by remember { mutableStateOf<LabelLayer?>(null) }
    // 현재 위치 마커 레이어 — Kakao Maps는 isMyLocationEnabled 없으므로 직접 관리합니다.
    var kakaoLocationLabelLayer by remember { mutableStateOf<LabelLayer?>(null) }
    // 현재 위치 마커 Label 참조 — 초기 표시(LaunchedEffect)와 GPS 이동(mapLineEvents) 양쪽에서 공유합니다.
    // [개념] 로컬 변수로 관리하면 두 LaunchedEffect가 각자 별도 Label을 추가하여 마커가 2개 생깁니다.
    //        Screen 레벨 state로 공유하면 동일한 Label을 추적하여 moveTo()가 올바르게 동작합니다.
    var kakaoLocationLabel by remember { mutableStateOf<Label?>(null) }

    // 권한 허용 시 기록을 즉시 시작할지 여부.
    // [개념] 탭 진입 시 권한 요청(false)과 기록 버튼 탭 시 권한 요청(true)을 구분합니다.
    //        launcher 콜백이 이 값을 참조하므로 launcher 선언보다 앞에 위치해야 합니다.
    var pendingStartRecording by remember { mutableStateOf(false) }
    // 위치 권한 보유 여부 상태.
    // [개념] 초기값은 현재 권한 상태로 설정합니다. 권한이 런타임에 허용되면 true로 업데이트되어
    //        LaunchedEffect(googleMap, locationPermissionGranted)가 재실행됩니다.
    //        이를 통해 권한 허용 직후 카메라 이동 + 내 위치 레이어 활성화가 동작합니다.
    var locationPermissionGranted by remember {
        mutableStateOf(LocationPermissionHelper.hasLocationPermission(context))
    }

    // 위치 권한 런타임 요청 런처.
    // [개념] rememberLauncherForActivityResult는 ActivityResult API를 Compose에서 사용합니다.
    //        결과 콜백에서 권한 허용 여부에 따라 분기합니다.
    //        iOS의 CLLocationManager requestWhenInUseAuthorization()에 대응합니다.
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            locationPermissionGranted = true
            // pendingStartRecording == true: 기록 버튼 탭 → 권한 허용 → 기록 즉시 시작
            // pendingStartRecording == false: 탭 진입 → 권한 허용 → 대기 (버튼은 활성화됨)
            if (pendingStartRecording) {
                pendingStartRecording = false
                viewModel.startRecording()
            }
        } else {
            pendingStartRecording = false
            // [개념] shouldShowRequestPermissionRationale은 Android 표준 2단계 권한 흐름의 핵심입니다.
            //        - true: 1차 거부 상태 → 앱 내 설명 팝업 후 재요청 가능
            //        - false: 영구 거부("다시 묻지 않음") 또는 최초 요청 전 → 시스템 설정으로 안내
            //        iOS에는 이 단계가 없으며, 거부 시 항상 설정 앱으로 안내합니다.
            val activity = context as? android.app.Activity
            val shouldShowRationale = activity?.shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) ?: false
            if (shouldShowRationale) {
                viewModel.showLocationRationalePopup()
            } else {
                viewModel.showLocationPermissionPopup()
            }
        }
    }

    // 카메라 권한 런처
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            val activity = context as? android.app.Activity
            val shouldShowRationale = activity?.shouldShowRequestPermissionRationale(
                Manifest.permission.CAMERA
            ) ?: false
            if (shouldShowRationale) {
                viewModel.showCameraRationalePopup()
            } else {
                viewModel.showCameraPermissionPopup()
            }
        }
    }

    // 카메라 촬영 런처.
    // [개념] TakePicture는 FileProvider URI를 전달하면 카메라 앱이 직접 원본 해상도로 파일을 저장합니다.
    //        TakePicturePreview와 달리 썸네일이 아닌 풀 해상도 이미지를 얻을 수 있습니다.
    //        iOS의 UIImagePickerController + sourceType: .camera에 대응합니다.
    //        촬영 직전에 생성한 파일명을 currentPhotoFileName에 저장해두고,
    //        성공(true) 시 ViewModel에 전달합니다.
    var currentPhotoFileName by remember { mutableStateOf("") }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoFileName.isNotEmpty()) {
            viewModel.savePhoto(currentPhotoFileName)
        }
        currentPhotoFileName = ""
    }

    // 지도에 이미 그려진 마커/사진마커 개수 추적 (새로 추가된 것만 그리기 위해 사용)
    // [개념] iOS의 RecordMapView.Coordinator의 lastMarkerCount / lastPhotoMarkerCount에 대응합니다.
    var lastDrawnMarkerCount by remember { mutableIntStateOf(0) }
    var lastDrawnPhotoMarkerCount by remember { mutableIntStateOf(0) }
    // 이전 isRecording 값 추적 (녹화 중단 시점 감지용)
    var wasRecording by remember { mutableStateOf(false) }

    // 포그라운드 복귀 직후 버스트 구간 제어 플래그.
    // [개념] 백그라운드에서 쌓인 GPS 이벤트들이 포그라운드 복귀 시 한꺼번에 처리되면
    //        addPolyline()이 N번 호출되어 경로가 하나씩 그려지는 애니메이션 현상이 발생합니다.
    //        ON_RESUME 시 이 플래그를 true로 설정하고, 버스트 구간에는 경로를 누적만 합니다.
    //        0.5초 debounce 완료 후 누적 경로를 1회 렌더링하고 플래그를 해제합니다.
    //        iOS의 needsFullRouteRefresh 플래그와 동일한 역할입니다.
    var needsFullRouteRefresh by remember { mutableStateOf(false) }
    // 버스트 구간에서 수신된 가장 최신 위치 좌표.
    // debounce 완료 시 카메라 이동 및 위치 마커 이동에 사용합니다.
    // [개념] iOS의 latestLocationDuringRefresh에 대응합니다.
    var latestLocationDuringBurst by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    // 포그라운드/백그라운드 전환 감지: ON_RESUME 시 버스트 처리 모드를 활성화합니다.
    // [개념] DisposableEffect + LifecycleEventObserver로 Activity Lifecycle 이벤트를 구독합니다.
    //        iOS의 didBecomeActive 알림 수신에 대응합니다.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                needsFullRouteRefresh = true
                latestLocationDuringBurst = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 동의 완료(또는 기동의) 후 ViewModel이 shouldProceedToRecording = true를 신호하면
    // Screen이 권한 체크 → 기록 시작 흐름을 실행합니다.
    // [개념] ViewModel은 직접 권한 요청을 할 수 없으므로 State 플래그로 Screen에 위임합니다.
    //        LaunchedEffect는 key 값이 false → true 로 바뀌는 순간만 재실행되므로
    //        처리 후 clearProceedToRecording()으로 false로 되돌려야 다음 번에도 동작합니다.
    LaunchedEffect(uiState.shouldProceedToRecording) {
        if (uiState.shouldProceedToRecording) {
            viewModel.clearProceedToRecording()
            if (LocationPermissionHelper.hasLocationPermission(context)) {
                viewModel.startRecording()
            } else {
                pendingStartRecording = true
                locationPermissionLauncher.launch(
                    LocationPermissionHelper.getPermissionsToRequest(context)
                )
            }
        }
    }

    // onAppear: 위치 모니터링 시작 + 위치 권한 즉시 요청.
    // [개념] LaunchedEffect(Unit)은 Composable이 처음 화면에 등장할 때 한 번 실행됩니다.
    //        iOS의 .onAppear { viewModel.startMonitoring() }에 대응합니다.
    //        권한이 없으면 탭 진입 즉시 시스템 다이얼로그를 표시합니다.
    //        pendingStartRecording = false: 권한 허용 후 기록을 자동 시작하지 않습니다.
    LaunchedEffect(Unit) {
        viewModel.startMonitoring()
        // 위치 또는 알림(Android 13+) 권한 중 하나라도 없으면 한 번에 요청합니다.
        val permissionsToRequest = LocationPermissionHelper.getPermissionsToRequest(context)
        if (permissionsToRequest.isNotEmpty()) {
            pendingStartRecording = false
            locationPermissionLauncher.launch(permissionsToRequest)
        }
    }

    // 초기 카메라 이동: 지도가 준비되거나 위치 권한이 허용되는 순간 실행합니다.
    //
    // [개념] key가 2개(googleMap, locationPermissionGranted)이므로 둘 중 하나가 바뀌면 재실행됩니다.
    //        권한 없이 지도가 먼저 로드된 경우에도, 권한 허용 직후 locationPermissionGranted가
    //        true로 바뀌면서 이 블록이 재실행되어 카메라 이동 + 내 위치 레이어가 활성화됩니다.
    //        iOS의 mapView(_:didUpdate:) + hasSetInitialRegion 패턴에 대응합니다.
    LaunchedEffect(googleMap, locationPermissionGranted) {
        val map = googleMap ?: return@LaunchedEffect
        if (!locationPermissionGranted) return@LaunchedEffect

        // 내 위치 파란 점 + 현재위치 아이콘 활성화
        // [개념] isMyLocationEnabled는 위치 권한이 있어야만 설정할 수 있습니다.
        //        onMapReady 시점에 권한이 없었다면 여기서 설정합니다.
        @Suppress("MissingPermission")
        map.isMyLocationEnabled = true

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // lastLocation: 캐시된 위치 (빠름, null 가능)
        // getCurrentLocation: 실제 GPS 요청 (느리지만 정확)
        // lastLocation을 먼저 시도하고, null이면 getCurrentLocation으로 폴백합니다.
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(location.latitude, location.longitude), 15f
                            )
                        )
                    } else {
                        // lastLocation이 null이면 현재 위치를 직접 요청
                        fusedLocationClient.getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY, null
                        ).addOnSuccessListener { currentLocation ->
                            currentLocation?.let {
                                map.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(it.latitude, it.longitude), 15f
                                    )
                                )
                            }
                        }
                    }
                }
        } catch (e: SecurityException) {
            // 권한 없음 — 위치 권한 팝업은 녹화 시작 버튼 클릭 시 처리
        }
    }

    // 경로선 그리기: mapLineEvents SharedFlow를 직접 구독합니다.
    //
    // [개념] 기존 LaunchedEffect(uiState.currentMapLine) 방식과의 차이:
    //        - 기존: collectAsStateWithLifecycle()을 통해 uiState를 받음
    //                → 앱이 백그라운드(Lifecycle STARTED 미만)일 때 수집 중단
    //                → 복귀 시 마지막 1개 값만 처리 → 긴 점프 선 발생
    //        - 신규: LaunchedEffect의 코루틴은 composable이 composition을 떠나기 전까지 유지됨
    //                백그라운드로 내려도 코루틴은 계속 실행되므로 모든 경로 이벤트를 처리함
    //        iOS의 UIViewRepresentable Coordinator가 라이프사이클 무관하게 동작하는 것과 동일합니다.
    //
    // [버스트 처리] 포그라운드 복귀 직후 쌓인 이벤트가 순차 처리되면 addPolyline()이 N번 호출되어
    //               경로가 하나씩 그려지는 애니메이션 현상이 발생합니다.
    //               needsFullRouteRefresh 구간에는 경로 데이터를 누적만 하고,
    //               0.5초 debounce 완료 후 누적 데이터를 1회 렌더링합니다.
    //               iOS의 appendPolyline() / renderPolylines() 분리 패턴에 대응합니다.
    LaunchedEffect(viewModel) {
        // [개념] LaunchedEffect 블록은 CoroutineScope이지만, collect { } 람다는 별도 suspend 람다입니다.
        //        collect 람다 안에서 launch { }를 호출하려면 외부 scope를 명시적으로 캡처해야 합니다.
        //        iOS의 handler.postDelayed()에 대응하는 코루틴 debounce 패턴입니다.
        val scope = this
        var debounceJob: Job? = null
        val pendingPolylines = mutableListOf<Pair<MapLineInfo, FDAppManager.FishingState>>()

        viewModel.mapLineEvents.collect { (mapLine, fishingState) ->
            val map = googleMap ?: return@collect
            if (!viewModel.uiState.value.isRecording) return@collect

            val prevLat = mapLine.previousLocation.latitude
            val prevLon = mapLine.previousLocation.longitude
            val currLat = mapLine.currentLocation.latitude
            val currLon = mapLine.currentLocation.longitude

            if (needsFullRouteRefresh) {
                // ── 버스트 구간: 렌더링 없이 누적만 ──────────────────────────────
                // [개념] iOS의 appendPolyline()에 대응합니다.
                if (prevLat != 0.0 && prevLon != 0.0 && currLat != 0.0 && currLon != 0.0) {
                    pendingPolylines.add(mapLine to fishingState)
                }
                if (currLat != 0.0 && currLon != 0.0) {
                    latestLocationDuringBurst = currLat to currLon
                }
                // debounce 재예약: 새 이벤트가 올 때마다 0.5초 타이머를 리셋합니다.
                // [개념] iOS의 scheduleRefreshPOI() debounce 구조에 대응합니다.
                debounceJob?.cancel()
                debounceJob = scope.launch {
                    delay(500)
                    // ── debounce 완료: 누적 경로 1회 렌더링 ──────────────────────
                    // [개념] iOS의 renderPolylines() + moveCameraToLocation()에 대응합니다.
                    val currentMap = googleMap
                    if (currentMap != null) {
                        pendingPolylines.forEach { (line, state) ->
                            val pLat = line.previousLocation.latitude
                            val pLon = line.previousLocation.longitude
                            val cLat = line.currentLocation.latitude
                            val cLon = line.currentLocation.longitude
                            val lineColor = stateLineColor(state)
                            currentMap.addPolyline(
                                PolylineOptions()
                                    .add(LatLng(pLat, pLon), LatLng(cLat, cLon))
                                    .width(8f)
                                    .color(lineColor.toArgb())
                                    .geodesic(false)
                            )
                        }
                        // 카메라를 현재 위치로 이동합니다.
                        latestLocationDuringBurst?.let { (lat, lon) ->
                            currentMap.animateCamera(
                                CameraUpdateFactory.newLatLng(LatLng(lat, lon))
                            )
                        }
                    }
                    pendingPolylines.clear()
                    latestLocationDuringBurst = null
                    needsFullRouteRefresh = false
                }
                return@collect
            }

            // ── 일반 구간: 기존 즉시 렌더링 ─────────────────────────────────────
            // 유효한 좌표 (0,0이 아닌 경우)일 때만 경로선 추가
            if (prevLat != 0.0 && prevLon != 0.0 && currLat != 0.0 && currLon != 0.0) {
                // [개념] fishingState는 이벤트 발행 시점의 상태값을 그대로 사용합니다.
                //        uiState.fishingState를 여기서 읽으면 현재 상태(이미 변경됐을 수 있음)를
                //        사용하게 되어 선분 색상이 잘못 적용될 수 있습니다.
                val lineColor = stateLineColor(fishingState)
                map.addPolyline(
                    PolylineOptions()
                        .add(LatLng(prevLat, prevLon), LatLng(currLat, currLon))
                        .width(8f)
                        .color(lineColor.toArgb())
                        .geodesic(false)
                )
            }
        }
    }

    // 상태 변경 마커 추가: markers 목록이 증가할 때만 새 마커를 추가합니다.
    // [개념] iOS의 if markers.count > coordinator.lastMarkerCount { } 패턴에 대응합니다.
    LaunchedEffect(uiState.markers.size) {
        val map = googleMap ?: return@LaunchedEffect
        if (uiState.markers.size <= lastDrawnMarkerCount) return@LaunchedEffect

        val newMarkers = uiState.markers.drop(lastDrawnMarkerCount)
        newMarkers.forEach { stateMarker ->
            val iconResId = when (stateMarker.state) {
                FDAppManager.FishingState.MOVING -> R.drawable.ic_map_marker_blue
                FDAppManager.FishingState.DRIFTING -> R.drawable.ic_map_marker_orange
                FDAppManager.FishingState.FISHING -> R.drawable.ic_map_marker_red
            }
            // [개념] BitmapFactory.decodeResource는 파일 I/O를 포함하는 작업으로 메인 스레드에서 실행하면
            //        프레임 드롭이 발생할 수 있습니다. withContext(Dispatchers.IO)로 백그라운드에서 디코딩 후
            //        결과만 메인 스레드로 가져와 addMarker()에 사용합니다.
            val bitmap = withContext(Dispatchers.IO) {
                BitmapFactory.decodeResource(context.resources, iconResId)
            }
            map.addMarker(
                MarkerOptions()
                    .position(LatLng(stateMarker.latitude, stateMarker.longitude))
                    .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                    // 마커 이미지의 하단 중앙이 좌표에 위치하도록 anchor 설정
                    // [개념] anchor(0.5f, 1.0f): X=중앙(0.5), Y=하단(1.0)
                    .anchor(0.5f, 1.0f)
            )
        }
        lastDrawnMarkerCount = uiState.markers.size
    }

    // 사진 마커 추가: photoMarkers 목록이 증가할 때 썸네일 마커를 추가합니다.
    // [개념] iOS의 addPhotoMarker()에 대응합니다.
    //        썸네일 이미지를 불러와 초록 테두리 + 둥근 모서리 스타일로 렌더링합니다.
    LaunchedEffect(uiState.photoMarkers.size) {
        val map = googleMap ?: return@LaunchedEffect
        if (uiState.photoMarkers.size <= lastDrawnPhotoMarkerCount) return@LaunchedEffect

        val newPhotoMarkers = uiState.photoMarkers.drop(lastDrawnPhotoMarkerCount)
        newPhotoMarkers.forEach { photoMarker ->
            // [개념] 썸네일 비트맵 생성(파일 읽기 + 캔버스 드로잉)을 IO 스레드에서 처리합니다.
            val thumbnailBitmap = withContext(Dispatchers.IO) {
                loadPhotoMarkerBitmap(context, photoMarker.thumbnailPath)
            }
            val icon = thumbnailBitmap?.let { BitmapDescriptorFactory.fromBitmap(it) }
                ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            map.addMarker(
                MarkerOptions()
                    .position(LatLng(photoMarker.latitude, photoMarker.longitude))
                    .icon(icon)
                    .anchor(0.5f, 1.0f)
            )
        }
        lastDrawnPhotoMarkerCount = uiState.photoMarkers.size
    }

    // 녹화 중단 감지: isRecording이 true → false로 바뀔 때 지도를 초기화합니다.
    // [개념] iOS의 kakaoMapAction = .clearMap / mapCoordinator?.clearMap() 호출에 대응합니다.
    //        Google/Kakao 초기화를 하나의 블록에서 처리하여 wasRecording 상태 경쟁을 방지합니다.
    LaunchedEffect(uiState.isRecording) {
        if (wasRecording && !uiState.isRecording) {
            // Google 지도 초기화
            googleMap?.clear()
            // Kakao 레이어 초기화 후 재생성
            // [개념] ShapeLayer/LabelLayer에는 인자 없는 remove()가 없으므로 매니저를 통해 제거합니다.
            // 경로/마커 레이어만 초기화합니다. 위치 레이어는 기록 상태와 무관하게 유지합니다.
            // [개념] 위치 레이어를 재생성하면 기존 마커가 사라집니다.
            //        기록 중단 후에도 현재 위치를 계속 표시해야 하므로
            //        위치 레이어(kakaoLocationLabelLayer, kakaoLocationLabel)는 건드리지 않습니다.
            kakaoMap?.let { m ->
                kakaoPolyLayer?.let { m.getShapeManager()?.remove(it) }
                kakaoStateLabelLayer?.let { m.getLabelManager()?.remove(it) }
                kakaoPhotoLabelLayer?.let { m.getLabelManager()?.remove(it) }
            }
            kakaoPolyLayer = null
            kakaoStateLabelLayer = null
            kakaoPhotoLabelLayer = null
            kakaoMap?.let { map ->
                kakaoPolyLayer = map.getShapeManager()?.addLayer(
                    ShapeLayerOptions.from("record_poly_layer").setZOrder(9999)
                )
                val labelMgr = map.getLabelManager()
                kakaoStateLabelLayer = labelMgr?.addLayer(
                    LabelLayerOptions.from("record_state_layer").setZOrder(1)
                )
                kakaoPhotoLabelLayer = labelMgr?.addLayer(
                    LabelLayerOptions.from("record_photo_layer").setZOrder(2)
                )
            }
            lastDrawnMarkerCount = 0
            lastDrawnPhotoMarkerCount = 0
        }
        wasRecording = uiState.isRecording
    }

    // 지도 타입 변경 시 두 맵 인스턴스와 드로잉 카운터를 초기화합니다.
    // [개념] currentMapType이 변경되면 기존 map 참조가 더 이상 유효하지 않으므로 null로 초기화합니다.
    //        낚시 기록 중에는 SettingViewModel에서 변경이 차단되므로 기록 데이터 손실이 없습니다.
    LaunchedEffect(currentMapType) {
        googleMap = null
        kakaoMap = null
        kakaoPolyLayer = null
        kakaoStateLabelLayer = null
        kakaoPhotoLabelLayer = null
        kakaoLocationLabelLayer = null
        kakaoLocationLabel = null
        lastDrawnMarkerCount = 0
        lastDrawnPhotoMarkerCount = 0
    }

    // ── Kakao 초기 카메라 이동 ─────────────────────────────────────────────
    // Google의 LaunchedEffect(googleMap, locationPermissionGranted)에 대응합니다.
    LaunchedEffect(kakaoMap, locationPermissionGranted) {
        val map = kakaoMap ?: return@LaunchedEffect
        if (!locationPermissionGranted) return@LaunchedEffect

        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    map.moveCamera(
                        KakaoCameraUpdateFactory.newCenterPosition(
                            KakaoLatLng.from(location.latitude, location.longitude), 15
                        )
                    )
                    // 현재 위치 마커 표시 (Google Maps의 파란 점 역할)
                    // [개념] Kakao Maps는 isMyLocationEnabled가 없으므로 직접 LabelLayer로 구현합니다.
                    //        반환된 Label을 공유 변수에 저장하여 mapLineEvents에서 moveTo()로 재사용합니다.
                    kakaoLocationLabel = showKakaoLocationMarker(
                        context, kakaoLocationLabelLayer,
                        location.latitude, location.longitude
                    )
                } else {
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY, null
                    ).addOnSuccessListener { current ->
                        current?.let {
                            map.moveCamera(
                                KakaoCameraUpdateFactory.newCenterPosition(
                                    KakaoLatLng.from(it.latitude, it.longitude), 15
                                )
                            )
                            kakaoLocationLabel = showKakaoLocationMarker(
                                context, kakaoLocationLabelLayer,
                                it.latitude, it.longitude
                            )
                        }
                    }
                }
            }
        } catch (e: SecurityException) { /* 권한 없음 */ }
    }

    // ── Kakao 경로선 그리기 ────────────────────────────────────────────────
    // Google의 LaunchedEffect(viewModel) { mapLineEvents.collect { } }에 대응합니다.
    // Google Maps와 동일한 버스트 처리 로직을 적용합니다.
    LaunchedEffect(viewModel) {
        // [개념] Label.moveTo()로 위치 마커를 이동합니다. 레이어를 재생성하지 않아 성능이 좋습니다.
        //        kakaoLocationLabel은 Screen 레벨 공유 변수입니다.
        //        LaunchedEffect(kakaoMap, locationPermissionGranted)에서 초기 마커를 추가하고
        //        이 coroutine에서 moveTo()로 갱신하여 마커가 중복 생성되지 않습니다.
        val scope = this
        var debounceJob: Job? = null
        val pendingPolylines = mutableListOf<Pair<MapLineInfo, FDAppManager.FishingState>>()

        viewModel.mapLineEvents.collect { (mapLine, fishingState) ->
            val layer = kakaoPolyLayer ?: return@collect
            if (!viewModel.uiState.value.isRecording) return@collect

            val prevLat = mapLine.previousLocation.latitude
            val prevLon = mapLine.previousLocation.longitude
            val currLat = mapLine.currentLocation.latitude
            val currLon = mapLine.currentLocation.longitude

            if (needsFullRouteRefresh) {
                // ── 버스트 구간: 렌더링/마커 이동 없이 누적만 ───────────────────
                if (prevLat != 0.0 && prevLon != 0.0 && currLat != 0.0 && currLon != 0.0) {
                    pendingPolylines.add(mapLine to fishingState)
                }
                if (currLat != 0.0 && currLon != 0.0) {
                    latestLocationDuringBurst = currLat to currLon
                }
                debounceJob?.cancel()
                debounceJob = scope.launch {
                    delay(500)
                    // ── debounce 완료: 누적 경로 1회 렌더링 + 마커/카메라 이동 ──
                    val currentLayer = kakaoPolyLayer
                    val currentMap = kakaoMap
                    if (currentLayer != null) {
                        pendingPolylines.forEach { (line, state) ->
                            val pLat = line.previousLocation.latitude
                            val pLon = line.previousLocation.longitude
                            val cLat = line.currentLocation.latitude
                            val cLon = line.currentLocation.longitude
                            val color = stateLineColor(state).toArgb()
                            // [개념] Kakao Maps는 MapPoints + PolylineStyle로 폴리라인을 정의합니다.
                            val mapPoints = MapPoints.fromLatLng(listOf(
                                KakaoLatLng.from(pLat, pLon),
                                KakaoLatLng.from(cLat, cLon)
                            ))
                            currentLayer.addPolyline(KakaoPolylineOptions.from(mapPoints, PolylineStyle.from(8f, color)))
                        }
                    }
                    // 현재 위치 마커 이동 및 카메라를 현재 위치로 이동합니다.
                    // [개념] iOS의 moveCurrentPoi(latest) + moveCameraToLocation()에 대응합니다.
                    latestLocationDuringBurst?.let { (lat, lon) ->
                        val existingLabel = kakaoLocationLabel
                        if (existingLabel != null) {
                            existingLabel.moveTo(KakaoLatLng.from(lat, lon))
                        } else {
                            kakaoLocationLabel = showKakaoLocationMarker(
                                context, kakaoLocationLabelLayer, lat, lon
                            )
                        }
                        currentMap?.moveCamera(
                            KakaoCameraUpdateFactory.newCenterPosition(KakaoLatLng.from(lat, lon))
                        )
                    }
                    pendingPolylines.clear()
                    latestLocationDuringBurst = null
                    needsFullRouteRefresh = false
                }
                return@collect
            }

            // ── 일반 구간: 기존 즉시 렌더링 ─────────────────────────────────────
            if (prevLat != 0.0 && prevLon != 0.0 && currLat != 0.0 && currLon != 0.0) {
                val color = stateLineColor(fishingState).toArgb()
                // [개념] Kakao Maps는 MapPoints + PolylineStyle로 폴리라인을 정의합니다.
                //        MapPoints.fromLatLng()에 좌표 컬렉션을 전달합니다.
                val mapPoints = MapPoints.fromLatLng(listOf(
                    KakaoLatLng.from(prevLat, prevLon),
                    KakaoLatLng.from(currLat, currLon)
                ))
                layer.addPolyline(KakaoPolylineOptions.from(mapPoints, PolylineStyle.from(8f, color)))
            }

            // 현재 위치 마커 갱신 — 기록 중 GPS 업데이트마다 위치 마커를 이동합니다.
            // [개념] Label.moveTo(LatLng)는 레이어를 재생성하지 않고 기존 마커 좌표만 변경합니다.
            if (currLat != 0.0 && currLon != 0.0) {
                val existingLabel = kakaoLocationLabel
                if (existingLabel != null) {
                    existingLabel.moveTo(KakaoLatLng.from(currLat, currLon))
                } else {
                    kakaoLocationLabel = showKakaoLocationMarker(
                        context, kakaoLocationLabelLayer, currLat, currLon
                    )
                }
            }
        }
    }

    // ── Kakao 상태 변경 마커 추가 ──────────────────────────────────────────
    // Google의 LaunchedEffect(uiState.markers.size)에 대응합니다.
    LaunchedEffect(uiState.markers.size) {
        val stateLabelLayer = kakaoStateLabelLayer ?: return@LaunchedEffect
        if (uiState.markers.size <= lastDrawnMarkerCount) return@LaunchedEffect

        val density = context.resources.displayMetrics.density
        val newMarkers = uiState.markers.drop(lastDrawnMarkerCount)
        newMarkers.forEach { stateMarker ->
            val iconResId = when (stateMarker.state) {
                FDAppManager.FishingState.MOVING -> R.drawable.ic_map_marker_blue
                FDAppManager.FishingState.DRIFTING -> R.drawable.ic_map_marker_orange
                FDAppManager.FishingState.FISHING -> R.drawable.ic_map_marker_red
            }
            // [개념] BitmapFactory.decodeResource + createScaledBitmap은 I/O 및 CPU 작업이므로
            //        withContext(Dispatchers.IO)로 백그라운드 처리합니다.
            val scaledBitmap = withContext(Dispatchers.IO) {
                val bitmap = BitmapFactory.decodeResource(context.resources, iconResId)
                // Kakao SDK는 비트맵 픽셀을 dp처럼 처리하므로 density로 나눠 Google Maps와 동일한 시각적 크기로 맞춥니다.
                // KakaoHistoryMapView의 상태 마커 스케일링과 동일한 로직입니다.
                android.graphics.Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width / density * 2).toInt(),
                    (bitmap.height / density * 2).toInt(),
                    true
                )
            }
            // [개념] Kakao Maps 마커는 LabelOptions에 LabelStyle을 설정하여 추가합니다.
            //        LabelStyle.from(Bitmap)으로 비트맵 기반 스타일을 직접 생성할 수 있습니다.
            val labelOptions = LabelOptions.from(
                KakaoLatLng.from(stateMarker.latitude, stateMarker.longitude)
            ).setStyles(LabelStyle.from(scaledBitmap))
            stateLabelLayer.addLabel(labelOptions)
        }
        lastDrawnMarkerCount = uiState.markers.size
    }

    // ── Kakao 사진 마커 추가 ───────────────────────────────────────────────
    // Google의 LaunchedEffect(uiState.photoMarkers.size)에 대응합니다.
    LaunchedEffect(uiState.photoMarkers.size) {
        val photoLabelLayer = kakaoPhotoLabelLayer ?: return@LaunchedEffect
        if (uiState.photoMarkers.size <= lastDrawnPhotoMarkerCount) return@LaunchedEffect

        val newPhotoMarkers = uiState.photoMarkers.drop(lastDrawnPhotoMarkerCount)
        newPhotoMarkers.forEach { photoMarker ->
            // Kakao SDK는 비트맵 픽셀을 dp처럼 처리하므로 density를 곱하지 않은 전용 함수를 사용합니다.
            // (Google Maps용 loadPhotoMarkerBitmap은 density를 곱한 px 값을 사용하여 Kakao에서 너무 크게 표시됨)
            // [개념] 썸네일 파일 읽기 + Canvas 드로잉을 IO 스레드에서 처리합니다.
            val thumbnailBitmap = withContext(Dispatchers.IO) {
                loadKakaoPhotoMarkerBitmap(context, photoMarker.thumbnailPath)
            } ?: return@forEach
            val labelOptions = LabelOptions.from(
                KakaoLatLng.from(photoMarker.latitude, photoMarker.longitude)
            ).setStyles(LabelStyle.from(thumbnailBitmap))
            photoLabelLayer.addLabel(labelOptions)
        }
        lastDrawnPhotoMarkerCount = uiState.photoMarkers.size
    }

    // onDisappear: 위치 모니터링 중단.
    // [개념] DisposableEffect의 onDispose는 Composable이 컴포지션을 떠날 때 실행됩니다.
    //        iOS의 .onDisappear { viewModel.stopMonitoring() }에 대응합니다.
    DisposableEffect(Unit) {
        onDispose { viewModel.stopMonitoring() }
    }

    // [개념] Box는 자식 Composable을 겹쳐 쌓는 레이아웃입니다.
    //        iOS의 ZStack { }과 동일합니다.
    Box(modifier = Modifier.fillMaxSize()) {

        // ── 1. 지도 레이어 ────────────────────────────────────────────────────
        // [개념] when(currentMapType)으로 Google/Kakao 지도를 선택합니다.
        //        currentMapType은 FDAppManager.mapTypeFlow를 구독하므로 설정 변경 시 자동 전환됩니다.
        //        낚시 기록 중에는 SettingViewModel에서 변경이 차단됩니다.
        when (currentMapType) {MapType.GOOGLE_MAP -> GoogleRecordMapView(
                modifier = Modifier.fillMaxSize(),
                onMapReady = { map ->
                    // [개념] GoogleRecordMapView의 update 블록이 리컴포지션마다 getMapAsync를 재호출하므로
                    //        onMapReady도 반복 실행됩니다. 이미 동일 인스턴스가 설정된 경우엔
                    //        초기화 코드를 건너뛰어 불필요한 재설정을 방지합니다.
                    if (googleMap == map) return@GoogleRecordMapView

                    googleMap = map
                    // 커스텀 UI를 사용하므로 기본 컨트롤 비활성화
                    map.uiSettings.isZoomControlsEnabled = false
                    map.uiSettings.isMyLocationButtonEnabled = false
                    map.uiSettings.isCompassEnabled = false
                    // isMyLocationEnabled는 LaunchedEffect(googleMap, locationPermissionGranted)에서 설정합니다.
                }
            )
            MapType.KAKAO_MAP -> KakaoRecordMapView(
                modifier = Modifier.fillMaxSize(),
                isVisible = isVisible,
                onMapReady = { map ->
                    if (kakaoMap == map) return@KakaoRecordMapView

                    kakaoMap = map
                    // [개념] Kakao Maps는 레이어 기반 구조입니다.
                    //        폴리라인 레이어(zOrder 9999), 상태 마커 레이어(1), 사진 마커 레이어(2) 순으로 생성합니다.
                    //        iOS의 RecordKakaoMapViewController 레이어 구조와 동일합니다.
                    kakaoPolyLayer = map.getShapeManager()?.addLayer(
                        ShapeLayerOptions.from("record_poly_layer").setZOrder(9999)
                    )
                    val labelMgr = map.getLabelManager()
                    kakaoStateLabelLayer = labelMgr?.addLayer(
                        LabelLayerOptions.from("record_state_layer").setZOrder(1)
                    )
                    kakaoPhotoLabelLayer = labelMgr?.addLayer(
                        LabelLayerOptions.from("record_photo_layer").setZOrder(2)
                    )
                    // 현재 위치 마커 레이어 — 모든 레이어 위에 표시합니다.
                    kakaoLocationLabelLayer = labelMgr?.addLayer(
                        LabelLayerOptions.from("record_location_layer").setZOrder(99999)
                    )
                    // 맵이 교체되면 이전 Label 참조가 유효하지 않으므로 초기화합니다.
                    kakaoLocationLabel = null
                }
            )
        }

        // ── 2. 오버레이 레이어 ────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {

            // 상단: TopInfoBar (좌측 상단 정렬, horizontal + vertical 패딩 각 16dp)
            TopInfoBar(
                uiState = uiState,
                onToggleSpeedUnit = viewModel::toggleSpeedUnit,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .fillMaxWidth()
            )

            // 우측: MapControlButtons
            // top: TopInfoBar top(16dp) + InfoBar height(~52dp) + gap(12dp) = 80dp
            MapControlButtons(
                onZoomIn = {
                    when (currentMapType) {
                        MapType.GOOGLE_MAP -> googleMap?.animateCamera(CameraUpdateFactory.zoomIn())
                        MapType.KAKAO_MAP -> kakaoMap?.moveCamera(KakaoCameraUpdateFactory.zoomIn())
                    }
                },
                onZoomOut = {
                    when (currentMapType) {
                        MapType.GOOGLE_MAP -> googleMap?.animateCamera(CameraUpdateFactory.zoomOut())
                        MapType.KAKAO_MAP -> kakaoMap?.moveCamera(KakaoCameraUpdateFactory.zoomOut())
                    }
                },
                onMyLocation = {
                    if (!LocationPermissionHelper.hasLocationPermission(context)) return@MapControlButtons
                    try {
                        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            if (location != null) {
                                when (currentMapType) {
                                    MapType.GOOGLE_MAP -> googleMap?.animateCamera(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(location.latitude, location.longitude), 15f
                                        )
                                    )
                                    MapType.KAKAO_MAP -> {
                                        kakaoMap?.moveCamera(
                                            KakaoCameraUpdateFactory.newCenterPosition(
                                                KakaoLatLng.from(location.latitude, location.longitude), 15
                                            )
                                        )
                                        // 카메라 이동 외에 현재 위치 마커도 갱신합니다.
                                        // 기록 중단 후 마커가 없는 경우 새로 추가하고, 이미 있으면 위치만 이동합니다.
                                        val existingLabel = kakaoLocationLabel
                                        if (existingLabel != null) {
                                            existingLabel.moveTo(KakaoLatLng.from(location.latitude, location.longitude))
                                        } else {
                                            kakaoLocationLabel = showKakaoLocationMarker(
                                                context, kakaoLocationLabelLayer,
                                                location.latitude, location.longitude
                                            )
                                        }
                                    }
                                }
                            } else {
                                fusedLocationClient.getCurrentLocation(
                                    Priority.PRIORITY_HIGH_ACCURACY, null
                                ).addOnSuccessListener { current ->
                                    current?.let {
                                        when (currentMapType) {
                                            MapType.GOOGLE_MAP -> googleMap?.animateCamera(
                                                CameraUpdateFactory.newLatLngZoom(
                                                    LatLng(it.latitude, it.longitude), 15f
                                                )
                                            )
                                            MapType.KAKAO_MAP -> {
                                                kakaoMap?.moveCamera(
                                                    KakaoCameraUpdateFactory.newCenterPosition(
                                                        KakaoLatLng.from(it.latitude, it.longitude), 15
                                                    )
                                                )
                                                val existingLabel = kakaoLocationLabel
                                                if (existingLabel != null) {
                                                    existingLabel.moveTo(KakaoLatLng.from(it.latitude, it.longitude))
                                                } else {
                                                    kakaoLocationLabel = showKakaoLocationMarker(
                                                        context, kakaoLocationLabelLayer,
                                                        it.latitude, it.longitude
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: SecurityException) { /* 권한 없음 */ }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 80.dp)
            )

            // 하단: BottomControlBar (화면 하단 중앙, 40dp bottom 패딩)
            BottomControlBar(
                uiState = uiState,
                onRecordClick = {
                    if (uiState.isRecording) {
                        viewModel.showStopPopup()
                    } else {
                        // 기록 시작 전 위치 수집 안내 팝업을 먼저 노출합니다.
                        // [개념] Google Play 정책상 위치 데이터를 수집하기 전
                        //        앱이 수집 목적을 명확히 고지(in-app disclosure)해야 합니다.
                        //        사용자가 "동의하고 시작"을 누른 후에 권한 요청으로 이어집니다.
                        viewModel.showLocationDisclosurePopup()
                    }
                },
                onCameraClick = {
                    val hasCameraPermission = context.checkSelfPermission(
                        Manifest.permission.CAMERA
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasCameraPermission) {
                        // [개념] TakePicture는 촬영 전에 저장할 파일 URI를 미리 지정합니다.
                        //        FileProvider는 앱 외부(카메라 앱)에 filesDir 경로를 content:// URI로 안전하게 공유합니다.
                        //        iOS의 PHPhotoLibrary.requestAuthorization 없이 filesDir에 직접 저장하는 것과 동일합니다.
                        val fileName = "${UUID.randomUUID()}.jpg"
                        val photoFile = File(context.filesDir, fileName)
                        val photoUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            photoFile
                        )
                        currentPhotoFileName = fileName
                        cameraLauncher.launch(photoUri)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
                    .fillMaxWidth()
            )
        }

        // ── 3. 팝업: 기록 중단 확인 ──────────────────────────────────────────
        // [개념] CommonPopupOverlay는 딤 배경 + 팝업 카드를 한 번에 처리합니다.
        //        iOS의 CommonPopupView(layoutType: .horizontal)에 대응합니다.
        if (uiState.isStopPopupPresented) {
            CommonPopupOverlay(
                title = "기록 중단",
                message = "현재 진행 중인 낚시 기록을 중단하시겠습니까?\n현재까지의 기록은 모두 저장됩니다.",
                layoutType = PopupLayoutType.Horizontal,
                primaryButtonText = "계속 기록",
                onPrimaryClick = { viewModel.dismissPopups() },
                secondaryButtonText = "중단",
                onSecondaryClick = { viewModel.stopRecording() },
                onDismiss = { viewModel.dismissPopups() }
            )
        }

        // ── 4. 팝업: 위치 권한 설명 (1차 거부 → 재요청) ─────────────────────
        // [개념] shouldShowRequestPermissionRationale == true일 때 표시합니다.
        //        왜 권한이 필요한지 설명하고 사용자에게 재요청 기회를 제공합니다.
        //        "허용하기"를 누르면 시스템 권한 다이얼로그를 다시 띄웁니다.
        if (uiState.isLocationRationalePopupPresented) {
            CommonPopupOverlay(
                title = "위치 권한이 필요합니다",
                message = "낚시 경로를 기록하려면 정확한 위치 접근이 필요합니다.\n권한을 허용하시겠습니까?",
                layoutType = PopupLayoutType.Horizontal,
                primaryButtonText = "허용하기",
                onPrimaryClick = {
                    viewModel.dismissPopups()
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                secondaryButtonText = "취소",
                onSecondaryClick = { viewModel.dismissPopups() },
                onDismiss = { viewModel.dismissPopups() }
            )
        }

        // ── 5. 팝업: 위치 권한 안내 (영구 거부 → 설정) ──────────────────────
        // [개념] shouldShowRequestPermissionRationale == false(영구 거부)일 때 표시합니다.
        //        앱에서 더 이상 권한 요청이 불가하므로 시스템 설정으로 안내합니다.
        //        iOS의 openURL(prefs:root=)에 대응합니다.
        if (uiState.isLocationPermissionPopupPresented) {
            CommonPopupOverlay(
                title = "위치 권한이 필요합니다",
                message = "낚시 기록을 위해 위치 권한이 필요합니다.\n설정에서 위치 서비스를 허용해주세요.",
                layoutType = PopupLayoutType.Vertical,
                primaryButtonText = "설정으로 이동",
                onPrimaryClick = {
                    viewModel.dismissPopups()
                    LocationPermissionHelper.openAppSettings(context)
                },
                secondaryButtonText = "취소",
                onSecondaryClick = { viewModel.dismissPopups() },
                onDismiss = { viewModel.dismissPopups() }
            )
        }

        // ── 6. 팝업: 카메라 권한 설명 (1차 거부 → 재요청) ───────────────────
        if (uiState.isCameraRationalePopupPresented) {
            CommonPopupOverlay(
                title = "카메라 권한이 필요합니다",
                message = "낚시 포인트 사진을 촬영하려면 카메라 접근이 필요합니다.\n권한을 허용하시겠습니까?",
                layoutType = PopupLayoutType.Horizontal,
                primaryButtonText = "허용하기",
                onPrimaryClick = {
                    viewModel.dismissPopups()
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                },
                secondaryButtonText = "취소",
                onSecondaryClick = { viewModel.dismissPopups() },
                onDismiss = { viewModel.dismissPopups() }
            )
        }

        // ── 7. 팝업: 카메라 권한 안내 (영구 거부 → 설정) ────────────────────
        if (uiState.isCameraPermissionPopupPresented) {
            CommonPopupOverlay(
                title = "카메라 권한 필요",
                message = "사진 촬영을 위해 카메라 접근 권한이 필요합니다.\n설정에서 권한을 허용해주세요.",
                layoutType = PopupLayoutType.Vertical,
                primaryButtonText = "설정으로 이동",
                onPrimaryClick = {
                    viewModel.dismissPopups()
                    LocationPermissionHelper.openAppSettings(context)
                },
                secondaryButtonText = "취소",
                onSecondaryClick = { viewModel.dismissPopups() },
                onDismiss = { viewModel.dismissPopups() }
            )
        }

        // ── 8. 팝업: 위치 정보 수집 안내 (기록 시작 전 in-app disclosure, 최초 1회) ─
        // [개념] Google Play 정책은 위치 데이터 수집 전 수집 목적을 명시적으로 고지하도록 요구합니다.
        //        "동의하고 시작" 탭 시 confirmLocationDisclosure()가 동의 여부를 SharedPreferences에 저장합니다.
        //        이후 실행부터는 팝업 없이 shouldProceedToRecording 플래그로 바로 기록 시작 흐름에 진입합니다.
        if (uiState.isLocationDisclosurePopupPresented) {
            CommonPopupOverlay(
                title = "위치 정보 수집 안내",
                message = "'온바다'는 낚시 이동 경로를 끊김 없이 기록하기 위해, 기록 중에는 화면이 꺼지거나 다른 앱으로 이동해도 GPS 위치 정보를 계속 수집합니다.\n수집된 위치 정보는 기기 내 낚시 기록 저장에만 사용되며, 기록을 종료하면 수집이 중단됩니다.",
                layoutType = PopupLayoutType.Vertical,
                primaryButtonText = "동의하고 시작",
                onPrimaryClick = { viewModel.confirmLocationDisclosure() },
                secondaryButtonText = "취소",
                onSecondaryClick = { viewModel.dismissPopups() },
                onDismiss = { viewModel.dismissPopups() }
            )
        }
    }
}

// ── Sub-views ────────────────────────────────────────────────────────────────

/**
 * 상단 정보 바 (상태 Dot + 상태 텍스트 + 속도/단위 토글 + 저장 지점 수).
 *
 * Figma: 흰색 배경(0.95 alpha), radius 16dp, 그림자(0px 2px 10px rgba(0,0,0,0.08))
 * 대기 중: 높이 ~47dp / 녹화 중: 높이 ~52dp (속도+토글로 인해 조금 더 높아짐)
 *
 * [개념] iOS의 topInfoBar 프로퍼티에 대응합니다.
 */
@Composable
private fun TopInfoBar(
    uiState: FishingRecordUiState,
    onToggleSpeedUnit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dotColor = statusDotColor(uiState)
    val dotAlpha = statusDotAlpha(uiState)
    val textColor = statusTextColor(uiState)
    val statusText = if (!uiState.isRecording) "대기 중" else uiState.fishingState.description

    // [개념] shadow → clip → background 순서가 중요합니다.
    //        shadow는 clip 이전에 적용되어야 외곽에 그림자가 렌더링됩니다.
    Row(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.95f))
            .height(52.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 좌측: 상태 정보 그룹 (Dot + 상태 텍스트 + 속도 + 단위 토글)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 상태 Dot (12dp 원형)
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = dotAlpha))
            )

            // 상태 텍스트
            Text(
                modifier = Modifier.offset(y = (-1).dp),
                text = statusText,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.23).sp
            )

            // 속도 + 단위 토글 (녹화 중일 때만 표시)
            if (uiState.isRecording) {
                val speedLabel = if (uiState.speedUnit == SpeedUnit.KNOTS) "knots" else "km/h"
                Text(
                    text = String.format("%.1f %s", uiState.convertedSpeed, speedLabel),
                    color = Color(0xFF8E8E93),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.08).sp
                )
                SpeedUnitToggleView(
                    selectedUnit = uiState.speedUnit,
                    onToggle = onToggleSpeedUnit
                )
            }
        }

        // 우측: 저장 지점 수
        // 녹화 중에는 공간 절약을 위해 "N개 지점" (Figma 기준, "저장" 생략)
        val pointCountText = if (uiState.isRecording) {
            "${uiState.savedPointCount}개 지점"
        } else {
            "${uiState.savedPointCount}개 지점 저장"
        }
        Text(
            modifier = Modifier.offset(y = (-1).dp),
            text = pointCountText,
            color = Color(0xFF8E8E93),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.08).sp
        )
    }
}

/**
 * 속도 단위 토글 버튼 (knots / km/h).
 *
 * Figma: 109dp × 28dp, 배경 rgba(0,0,0,0.06), radius 12dp
 *        활성 버튼: 흰색 배경 + 그림자 / 비활성 버튼: 투명 배경
 */
@Composable
private fun SpeedUnitToggleView(
    selectedUnit: SpeedUnit,
    onToggle: () -> Unit
) {
    // [개념] contentAlignment = Center로 설정해야 내부 Row가 Box 높이 기준으로 중앙에 위치합니다.
    //        기본값 TopStart이면 Row가 상단에 붙어 외부 Row의 CenterVertically와 어긋납니다.
    Box(
        modifier = Modifier
            .width(109.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.06f))
            .padding(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // knots 버튼 (54dp)
            SpeedUnitButton(
                label = "knots",
                isSelected = selectedUnit == SpeedUnit.KNOTS,
                width = 54.dp,
                onClick = { if (selectedUnit != SpeedUnit.KNOTS) onToggle() }
            )
            // km/h 버튼 (50dp)
            SpeedUnitButton(
                label = "km/h",
                isSelected = selectedUnit == SpeedUnit.KMH,
                width = 50.dp,
                onClick = { if (selectedUnit != SpeedUnit.KMH) onToggle() }
            )
        }
    }
}

/**
 * 속도 단위 토글 내 개별 버튼.
 *
 * [개념] isSelected에 따라 흰색 배경 + 그림자가 있는 활성 스타일과
 *        투명 배경의 비활성 스타일로 분기합니다.
 */
@Composable
private fun SpeedUnitButton(
    label: String,
    isSelected: Boolean,
    width: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val baseModifier = Modifier
        .width(width)
        .height(24.dp)
        .clip(RoundedCornerShape(10.dp))
        .clickable(onClick = onClick)

    val selectedModifier = Modifier
        .width(width)
        .height(24.dp)
        .shadow(
            elevation = 1.dp,
            shape = RoundedCornerShape(10.dp),
            spotColor = Color.Black.copy(alpha = 0.1f),
            ambientColor = Color.Black.copy(alpha = 0.05f)
        )
        .clip(RoundedCornerShape(10.dp))
        .background(Color.White)
        .clickable(onClick = onClick)

    Box(
        modifier = if (isSelected) selectedModifier else baseModifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF1F2937) else Color(0xFF6B7280),
            letterSpacing = 0.06.sp
        )
    }
}

/**
 * 지도 컨트롤 버튼 그룹 (줌인 / 줌아웃 / 내 위치).
 *
 * Figma: 48dp 원형, 흰색 배경(0.95 alpha), 20dp 아이콘, 8dp 간격, 그림자
 * iOS의 mapControlButtons에 대응합니다.
 */
@Composable
private fun MapControlButtons(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onMyLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircleIconButton(iconResId = R.drawable.btn_zoom_in, contentDescription = "줌인", onClick = onZoomIn)
        CircleIconButton(iconResId = R.drawable.btn_zoom_out, contentDescription = "줌아웃", onClick = onZoomOut)
        CircleIconButton(iconResId = R.drawable.btn_my_location, contentDescription = "내 위치", onClick = onMyLocation)
    }
}

/**
 * 원형 아이콘 버튼 (지도 컨트롤 공통 스타일).
 *
 * Figma: 48dp 원형, 흰색 배경(rgba(255,255,255,0.95)), 그림자(0px 2px 10px rgba(0,0,0,0.08)), 아이콘 20dp
 */
@Composable
private fun CircleIconButton(
    iconResId: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .shadow(
                elevation = 4.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.95f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = Color.Black
        )
    }
}

/**
 * 하단 컨트롤 바 (안내 텍스트 + 녹화 버튼 + 카메라 버튼).
 *
 * Figma: 안내 텍스트(대기 중만 표시), 녹화 버튼(중앙 64dp), 카메라 버튼(우측 56dp)
 * iOS의 bottomControlBar에 대응합니다.
 */
@Composable
private fun BottomControlBar(
    uiState: FishingRecordUiState,
    onRecordClick: () -> Unit,
    onCameraClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 안내 텍스트 pill (녹화 전 대기 중일 때만 표시)
        if (!uiState.isRecording) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "낚시를 시작하려면 버튼을 누르세요",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.08).sp
                )
            }
        }

        // 버튼 행 (녹화 버튼 중앙, 카메라 버튼 우측)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp)
        ) {
            RecordButton(
                isRecording = uiState.isRecording,
                onClick = onRecordClick,
                modifier = Modifier.align(Alignment.Center)
            )
            CameraButton(
                isEnabled = uiState.isRecording,
                onClick = onCameraClick,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

/**
 * 녹화 시작/중단 버튼.
 *
 * Figma: 64dp 원형, 대기 중 초록(#10B981) / 녹화 중 빨강(#EF4444)
 *        그림자(0px 4px 20px rgba(0,0,0,0.2))
 */
@Composable
private fun RecordButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonColor = if (isRecording) Color(0xFFEF4444) else Color(0xFF10B981)
    val iconResId = if (isRecording) R.drawable.btn_record_stop else R.drawable.btn_record_play

    // 84dp 터치 영역 안에 64dp 버튼 배치 (iOS와 동일한 터치 영역 확보)
    Box(
        modifier = modifier.size(84.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.2f),
                    spotColor = Color.Black.copy(alpha = 0.2f)
                )
                .clip(CircleShape)
                .background(buttonColor)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = if (isRecording) "기록 중단" else "기록 시작",
                modifier = Modifier.size(28.dp),
                tint = Color.White
            )
        }
    }
}

/**
 * 카메라 버튼.
 *
 * Figma: 56dp 원형, 흰색 배경(0.95 alpha), 24dp 아이콘
 *        대기 중 opacity 40%, 녹화 중 opacity 100%
 */
@Composable
private fun CameraButton(
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonAlpha = if (isEnabled) 1f else 0.4f

    Box(
        modifier = modifier
            .size(56.dp)
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.15f * buttonAlpha),
                spotColor = Color.Black.copy(alpha = 0.15f * buttonAlpha)
            )
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.95f * buttonAlpha))
            .clickable(enabled = isEnabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.btn_camera),
            contentDescription = "사진 촬영",
            modifier = Modifier.size(24.dp),
            tint = Color.Black.copy(alpha = buttonAlpha)
        )
    }
}

// ── 팝업 ─────────────────────────────────────────────────────────────────────

// ── Helper Functions ──────────────────────────────────────────────────────────

/**
 * 현재 상태에 따른 Dot 색상을 반환합니다.
 * 대기 중: 회색-파랑 / 이동 중: 파랑 / 탐색 중: 주황 / 낚시 중: 빨강
 */
private fun statusDotColor(uiState: FishingRecordUiState): Color {
    if (!uiState.isRecording) return StateIdleColor
    return when (uiState.fishingState) {
        FDAppManager.FishingState.MOVING -> StateMovingColor
        FDAppManager.FishingState.DRIFTING -> StateDriftingColor
        FDAppManager.FishingState.FISHING -> StateFishingColor
    }
}

/**
 * 현재 상태에 따른 Dot 불투명도(alpha)를 반환합니다.
 * Figma: 대기 중 1.0 / 이동 중 0.58 / 탐색 중 0.60 / 낚시 중 0.71
 */
private fun statusDotAlpha(uiState: FishingRecordUiState): Float {
    if (!uiState.isRecording) return 1.0f
    return when (uiState.fishingState) {
        FDAppManager.FishingState.MOVING -> 0.58f
        FDAppManager.FishingState.DRIFTING -> 0.60f
        FDAppManager.FishingState.FISHING -> 0.71f
    }
}

/**
 * 현재 상태에 따른 상태 텍스트 색상을 반환합니다.
 * 대기 중: Slate-500(#64748B) / 녹화 중: 상태별 색상
 */
private fun statusTextColor(uiState: FishingRecordUiState): Color {
    if (!uiState.isRecording) return StateIdleTextColor
    return when (uiState.fishingState) {
        FDAppManager.FishingState.MOVING -> StateMovingColor
        FDAppManager.FishingState.DRIFTING -> StateDriftingColor
        FDAppManager.FishingState.FISHING -> StateFishingColor
    }
}

/**
 * 낚시 상태에 따른 경로선 색상을 반환합니다.
 * iOS의 RecordMapView.Coordinator.getColor(for:)에 대응합니다.
 *
 * MOVING: 파랑 #2563EB / DRIFTING: 주황 #F59E0B / FISHING: 빨강 #EF4444
 */
private fun stateLineColor(fishingState: FDAppManager.FishingState): Color {
    return when (fishingState) {
        FDAppManager.FishingState.MOVING -> StateMovingColor
        FDAppManager.FishingState.DRIFTING -> StateDriftingColor
        FDAppManager.FishingState.FISHING -> StateFishingColor
    }
}

/**
 * 사진 마커 Bitmap 생성.
 *
 * iOS의 generateThumbnail() 렌더링 로직에 대응합니다.
 * 썸네일 이미지를 48dp 정사각형으로 리사이즈하고,
 * 초록 테두리(#10B981, 3dp)와 둥근 모서리(10dp)를 적용합니다.
 *
 * [개념] Android Bitmap은 픽셀(px) 단위로 동작합니다.
 *        iOS는 포인트(pt) + scale(2.0)으로 고밀도 이미지를 생성하지만,
 *        Android는 dp 값에 화면 밀도(density)를 직접 곱해 px로 변환해야
 *        동일한 물리적 크기로 마커가 표시됩니다.
 *
 * @param context 앱 Context
 * @param thumbnailPath Documents 디렉토리 기준 상대 경로
 * @return 마커용 Bitmap (파일 없으면 null)
 */
private fun loadPhotoMarkerBitmap(context: Context, thumbnailPath: String): Bitmap? {
    // 썸네일(uuid_thumb.jpg)을 우선 로드합니다. 없으면 원본(uuid.jpg)으로 폴백합니다.
    // [이유] 기존 레코드(업데이트 이전)에는 uuid_thumb.jpg가 없으므로 폴백이 필요합니다.
    val thumbPath = thumbnailPath.removeSuffix(".jpg") + "_thumb.jpg"
    val file = File(context.filesDir, thumbPath).takeIf { it.exists() }
        ?: File(context.filesDir, thumbnailPath).takeIf { it.exists() }
        ?: return null

    val originalBitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null

    // [개념] dp → px 변환: density를 곱해야 화면 밀도에 관계없이 동일한 물리적 크기로 표시됩니다.
    //        iOS의 format.scale = 2.0과 같은 역할입니다.
    //        예) xxhdpi(density=3.0) 기기에서 48dp → 144px
    val density = context.resources.displayMetrics.density
    val imageSize = (48 * density).toInt()
    val borderWidth = (3 * density).toInt()
    val cornerRadius = 10f * density
    val totalSize = imageSize + borderWidth * 2

    val result = Bitmap.createBitmap(totalSize, totalSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 1단계: 초록 테두리 배경 전체 그리기 (Figma: #10B981)
    paint.color = android.graphics.Color.parseColor("#10B981")
    canvas.drawRoundRect(
        RectF(0f, 0f, totalSize.toFloat(), totalSize.toFloat()),
        cornerRadius + borderWidth,
        cornerRadius + borderWidth,
        paint
    )

    // 2단계: 이미지 영역을 둥근 사각형으로 클리핑 후 Aspect Fill 방식으로 그리기.
    // [개념] canvas.save() / clipPath() / restore() 패턴은 iOS의 clipPath.addClip()에 대응합니다.
    //        클리핑 범위를 imageRect로 제한하므로 초록 테두리 영역은 영향받지 않습니다.
    //        이전 DST_IN 방식은 테두리까지 투명하게 만드는 버그가 있었습니다.
    val imageRect = RectF(
        borderWidth.toFloat(), borderWidth.toFloat(),
        (borderWidth + imageSize).toFloat(), (borderWidth + imageSize).toFloat()
    )

    canvas.save()
    // 이미지 영역만 둥근 사각형으로 클리핑
    val clipPath = android.graphics.Path().apply {
        addRoundRect(imageRect, cornerRadius, cornerRadius, android.graphics.Path.Direction.CW)
    }
    canvas.clipPath(clipPath)

    // Aspect Fill: iOS의 max(widthRatio, heightRatio) 로직과 동일합니다.
    // [개념] scaledToFill과 같이 짧은 변 기준으로 확대하여 여백 없이 채웁니다.
    val srcWidth = originalBitmap.width.toFloat()
    val srcHeight = originalBitmap.height.toFloat()
    val dstSize = imageSize.toFloat()
    val scale = maxOf(dstSize / srcWidth, dstSize / srcHeight)
    val drawnWidth = srcWidth * scale
    val drawnHeight = srcHeight * scale
    val drawnX = imageRect.left + (dstSize - drawnWidth) / 2f
    val drawnY = imageRect.top + (dstSize - drawnHeight) / 2f

    paint.reset()
    canvas.drawBitmap(
        originalBitmap,
        null,
        RectF(drawnX, drawnY, drawnX + drawnWidth, drawnY + drawnHeight),
        paint
    )
    canvas.restore()

    return result
}

/**
 * Kakao 지도에 현재 위치 마커를 추가합니다.
 *
 * [개념] Google Maps의 isMyLocationEnabled = true가 표시하는 파란 점에 대응합니다.
 *        Kakao Maps SDK에는 내장 현재 위치 표시 기능이 없으므로 LabelLayer를 직접 관리합니다.
 *        초기 위치 표시(최초 진입)와 기록 중 위치 업데이트(moveTo) 두 곳에서 사용합니다.
 *
 * @param context          앱 Context (리소스 로드용)
 * @param locationLayer    위치 마커를 추가할 LabelLayer (null이면 no-op)
 * @param latitude         현재 위도
 * @param longitude        현재 경도
 * @return 추가된 Label 인스턴스 (moveTo 갱신에 재사용), 레이어 없으면 null
 */
private fun showKakaoLocationMarker(
    context: Context,
    locationLayer: LabelLayer?,
    latitude: Double,
    longitude: Double
): Label? {
    val layer = locationLayer ?: return null
    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.map_ico_marker)
    // [개념] setAnchorPoint(0.5f, 0.5f)는 이미지의 정중앙을 GPS 좌표에 맞춥니다.
    //        기본값은 하단 중앙(0.5f, 1.0f)으로, 원형 아이콘이 실제 좌표보다 위로 올라가 보이는 원인입니다.
    //        경로라인 위에 아이콘 중앙이 정확히 위치하도록 중앙 앵커로 수정합니다.
    return layer.addLabel(
        LabelOptions.from(KakaoLatLng.from(latitude, longitude))
            .setStyles(LabelStyle.from(bitmap).setAnchorPoint(0.5f, 0.5f))
    )
}

/**
 * Kakao Maps용 사진 마커 Bitmap 생성.
 *
 * KakaoHistoryMapView의 loadHistoryPhotoMarkerBitmap과 동일한 로직입니다.
 * Google Maps용 loadPhotoMarkerBitmap과 달리 density를 곱하지 않습니다.
 * Kakao SDK는 비트맵 픽셀을 dp처럼 해석하므로, density를 곱하면 3배 이상 크게 표시됩니다.
 *
 * @param context 앱 Context
 * @param thumbnailPath Documents 디렉토리 기준 상대 경로
 * @return 마커용 Bitmap (파일 없으면 null)
 */
private fun loadKakaoPhotoMarkerBitmap(context: Context, thumbnailPath: String): Bitmap? {
    val thumbPath = thumbnailPath.removeSuffix(".jpg") + "_thumb.jpg"
    val file = File(context.filesDir, thumbPath).takeIf { it.exists() }
        ?: File(context.filesDir, thumbnailPath).takeIf { it.exists() }
        ?: return null

    val originalBitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null

    // Kakao SDK는 비트맵 픽셀을 dp처럼 처리하므로, density를 곱하지 않고 dp 값 그대로 사용합니다.
    val imageSize    = 96
    val borderWidth  = 6
    val cornerRadius = 20f
    val totalSize    = imageSize + borderWidth * 2

    val result = Bitmap.createBitmap(totalSize, totalSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint  = Paint(Paint.ANTI_ALIAS_FLAG)

    // 1. 초록 테두리 배경 (Figma: #10B981)
    paint.color = android.graphics.Color.parseColor("#10B981")
    canvas.drawRoundRect(
        RectF(0f, 0f, totalSize.toFloat(), totalSize.toFloat()),
        cornerRadius + borderWidth,
        cornerRadius + borderWidth,
        paint
    )

    // 2. 이미지 영역 클리핑 후 Aspect Fill 방식으로 그리기
    val imageRect = RectF(
        borderWidth.toFloat(), borderWidth.toFloat(),
        (borderWidth + imageSize).toFloat(), (borderWidth + imageSize).toFloat()
    )
    canvas.save()
    val clipPath = android.graphics.Path().apply {
        addRoundRect(imageRect, cornerRadius, cornerRadius, android.graphics.Path.Direction.CW)
    }
    canvas.clipPath(clipPath)

    val srcWidth  = originalBitmap.width.toFloat()
    val srcHeight = originalBitmap.height.toFloat()
    val dstSize   = imageSize.toFloat()
    val scale     = maxOf(dstSize / srcWidth, dstSize / srcHeight)
    val drawnWidth  = srcWidth * scale
    val drawnHeight = srcHeight * scale
    val drawnX = imageRect.left + (dstSize - drawnWidth) / 2f
    val drawnY = imageRect.top  + (dstSize - drawnHeight) / 2f

    paint.reset()
    canvas.drawBitmap(
        originalBitmap,
        null,
        RectF(drawnX, drawnY, drawnX + drawnWidth, drawnY + drawnHeight),
        paint
    )
    canvas.restore()

    return result
}
