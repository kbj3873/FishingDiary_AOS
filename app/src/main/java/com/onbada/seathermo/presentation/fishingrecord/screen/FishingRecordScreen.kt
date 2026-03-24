package com.onbada.seathermo.presentation.fishingrecord.screen

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.onbada.seathermo.R
import com.onbada.seathermo.managers.FDAppManager
import com.onbada.seathermo.presentation.fishingrecord.screen.components.GoogleRecordMapView
import com.onbada.seathermo.presentation.fishingrecord.viewmodel.FishingRecordUiState
import com.onbada.seathermo.presentation.fishingrecord.viewmodel.FishingRecordViewModel
import com.onbada.seathermo.presentation.fishingrecord.viewmodel.SpeedUnit
import com.onbada.seathermo.utility.LocationPermissionHelper
import java.io.ByteArrayOutputStream
import java.io.File

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
fun FishingRecordScreen(viewModel: FishingRecordViewModel) {
    // [개념] collectAsStateWithLifecycle()은 앱이 백그라운드 상태일 때 Flow 수집을 자동 중단합니다.
    //        Swift의 @ObservedObject와 동일하게 UI가 자동으로 상태를 반영합니다.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // GoogleMap 인스턴스 보관 (지도 컨트롤 버튼에서 사용)
    // [개념] var googleMap by remember { mutableStateOf<GoogleMap?>(null) }은
    //        지도가 준비되면 참조를 저장합니다. null인 동안 컨트롤 버튼 클릭은 무시됩니다.
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }

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
    // [개념] TakePicturePreview는 촬영된 Bitmap을 결과로 반환합니다.
    //        ViewModel이 ByteArray를 받으므로 Bitmap → JPEG ByteArray로 변환합니다.
    //        iOS의 UIImagePickerController에 대응합니다.
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val stream = ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            viewModel.savePhoto(stream.toByteArray())
        }
    }

    // 지도에 이미 그려진 마커/사진마커 개수 추적 (새로 추가된 것만 그리기 위해 사용)
    // [개념] iOS의 RecordMapView.Coordinator의 lastMarkerCount / lastPhotoMarkerCount에 대응합니다.
    var lastDrawnMarkerCount by remember { mutableIntStateOf(0) }
    var lastDrawnPhotoMarkerCount by remember { mutableIntStateOf(0) }
    // 이전 isRecording 값 추적 (녹화 중단 시점 감지용)
    var wasRecording by remember { mutableStateOf(false) }

    // onAppear: 위치 모니터링 시작 + 위치 권한 즉시 요청.
    // [개념] LaunchedEffect(Unit)은 Composable이 처음 화면에 등장할 때 한 번 실행됩니다.
    //        iOS의 .onAppear { viewModel.startMonitoring() }에 대응합니다.
    //        권한이 없으면 탭 진입 즉시 시스템 다이얼로그를 표시합니다.
    //        pendingStartRecording = false: 권한 허용 후 기록을 자동 시작하지 않습니다.
    LaunchedEffect(Unit) {
        viewModel.startMonitoring()
        if (!LocationPermissionHelper.hasLocationPermission(context)) {
            pendingStartRecording = false
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
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

    // 경로선 그리기: currentMapLine이 변경될 때마다 선분을 추가합니다.
    // [개념] LaunchedEffect(key)는 key 값이 바뀔 때마다 재실행됩니다.
    //        iOS의 RecordMapView.updateUIView에서 addPolyline()을 호출하는 것에 대응합니다.
    LaunchedEffect(uiState.currentMapLine) {
        val mapLine = uiState.currentMapLine ?: return@LaunchedEffect
        val map = googleMap ?: return@LaunchedEffect
        if (!uiState.isRecording) return@LaunchedEffect

        val prevLat = mapLine.previousLocation.latitude
        val prevLon = mapLine.previousLocation.longitude
        val currLat = mapLine.currentLocation.latitude
        val currLon = mapLine.currentLocation.longitude

        // 유효한 좌표 (0,0이 아닌 경우)일 때만 경로선 추가
        if (prevLat != 0.0 && prevLon != 0.0 && currLat != 0.0 && currLon != 0.0) {
            val lineColor = stateLineColor(uiState.fishingState)
            map.addPolyline(
                PolylineOptions()
                    .add(LatLng(prevLat, prevLon), LatLng(currLat, currLon))
                    .width(8f)
                    .color(lineColor.toArgb())
                    .geodesic(false)
            )
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
            val bitmap = BitmapFactory.decodeResource(context.resources, iconResId)
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
            val thumbnailBitmap = loadPhotoMarkerBitmap(context, photoMarker.thumbnailPath)
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
    LaunchedEffect(uiState.isRecording) {
        if (wasRecording && !uiState.isRecording) {
            googleMap?.clear()
            lastDrawnMarkerCount = 0
            lastDrawnPhotoMarkerCount = 0
        }
        wasRecording = uiState.isRecording
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
        GoogleRecordMapView(
            modifier = Modifier.fillMaxSize(),
            onMapReady = { map ->
                googleMap = map
                // 커스텀 UI를 사용하므로 기본 컨트롤 비활성화
                map.uiSettings.isZoomControlsEnabled = false
                map.uiSettings.isMyLocationButtonEnabled = false
                map.uiSettings.isCompassEnabled = false
                // isMyLocationEnabled는 LaunchedEffect(googleMap, locationPermissionGranted)에서 설정합니다.
                // 권한 없이 탭에 진입한 경우에도 권한 허용 직후 재실행되어 정상 처리됩니다.
            }
        )

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
                onZoomIn = { googleMap?.animateCamera(CameraUpdateFactory.zoomIn()) },
                onZoomOut = { googleMap?.animateCamera(CameraUpdateFactory.zoomOut()) },
                onMyLocation = {
                    val map = googleMap ?: return@MapControlButtons
                    if (!LocationPermissionHelper.hasLocationPermission(context)) return@MapControlButtons
                    try {
                        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            if (location != null) {
                                map.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(location.latitude, location.longitude), 15f
                                    )
                                )
                            } else {
                                fusedLocationClient.getCurrentLocation(
                                    Priority.PRIORITY_HIGH_ACCURACY, null
                                ).addOnSuccessListener { current ->
                                    current?.let {
                                        map.animateCamera(
                                            CameraUpdateFactory.newLatLngZoom(
                                                LatLng(it.latitude, it.longitude), 15f
                                            )
                                        )
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
                        // 위치 권한 체크 → 없으면 시스템 권한 다이얼로그 요청
                        if (LocationPermissionHelper.hasLocationPermission(context)) {
                            viewModel.startRecording()
                        } else {
                            // pendingStartRecording = true: 권한 허용 즉시 기록 시작
                            pendingStartRecording = true
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }
                },
                onCameraClick = {
                    val hasCameraPermission = context.checkSelfPermission(
                        Manifest.permission.CAMERA
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasCameraPermission) {
                        cameraLauncher.launch(null)
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
 * iOS의 photoAnnotationView() 렌더링 로직에 대응합니다.
 * 썸네일 이미지를 48dp 정사각형으로 리사이즈하고,
 * 초록 테두리(#10B981, 3dp)와 둥근 모서리(10dp)를 적용합니다.
 *
 * @param context 앱 Context
 * @param thumbnailPath Documents 디렉토리 기준 상대 경로
 * @return 마커용 Bitmap (파일 없으면 null)
 */
private fun loadPhotoMarkerBitmap(context: Context, thumbnailPath: String): Bitmap? {
    val file = File(context.filesDir, thumbnailPath)
    if (!file.exists()) return null

    val originalBitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null

    // 48px 정사각형으로 리사이즈 (Figma 기준 48dp)
    val imageSize = 48
    val borderWidth = 3
    val cornerRadius = 10f
    val totalSize = imageSize + borderWidth * 2

    val resized = Bitmap.createScaledBitmap(originalBitmap, imageSize, imageSize, true)
    val result = Bitmap.createBitmap(totalSize, totalSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)

    // 초록 테두리 배경 (Figma: #10B981)
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#10B981")
    }
    canvas.drawRoundRect(
        RectF(0f, 0f, totalSize.toFloat(), totalSize.toFloat()),
        cornerRadius + borderWidth,
        cornerRadius + borderWidth,
        borderPaint
    )

    // 썸네일 이미지 (둥근 모서리로 클리핑)
    val imageRect = RectF(
        borderWidth.toFloat(), borderWidth.toFloat(),
        (borderWidth + imageSize).toFloat(), (borderWidth + imageSize).toFloat()
    )
    val clipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // DST_IN: 클리핑 마스크 역할
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    // 먼저 이미지 그리기
    canvas.drawBitmap(resized, null, imageRect, null)
    // 둥근 사각형 마스크로 클리핑
    val maskBitmap = Bitmap.createBitmap(totalSize, totalSize, Bitmap.Config.ARGB_8888)
    val maskCanvas = Canvas(maskBitmap)
    val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    maskCanvas.drawRoundRect(imageRect, cornerRadius, cornerRadius, maskPaint)
    canvas.drawBitmap(maskBitmap, 0f, 0f, clipPaint)

    return result
}
