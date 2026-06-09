package com.onbada.seathermo.presentation.history.screen

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng as KakaoLatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory as KakaoCameraUpdateFactory
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
import com.onbada.seathermo.managers.FDAppManager
import com.onbada.seathermo.presentation.history.viewmodel.GeoCoord
import com.onbada.seathermo.presentation.history.viewmodel.HistoryEndpointMarker
import com.onbada.seathermo.presentation.history.viewmodel.HistoryPhotoMarker
import com.onbada.seathermo.presentation.history.viewmodel.HistoryPolyline
import com.onbada.seathermo.presentation.history.viewmodel.HistoryStateMarker

// ── 폴리라인 색상 (GoogleHistoryMapView와 동일) ──────────────────────────────
private val KakaoHistoryMovingColor   = Color(0xFF2563EB)  // 이동: 파랑
private val KakaoHistoryDriftingColor = Color(0xFFF59E0B)  // 탐색: 주황
private val KakaoHistoryFishingColor  = Color(0xFFEF4444)  // 낚시: 빨강

/**
 * 히스토리 상세 화면용 Kakao Maps 컴포저블.
 *
 * [개념] GoogleHistoryMapView와 동일한 파라미터를 받아 Kakao Maps SDK로 렌더링합니다.
 *        HistoryDetailScreen에서 FDAppManager.mapType에 따라 이 컴포넌트가 선택됩니다.
 *        iOS의 HistoryKakaoMapViewController.swift 로직을 Android 환경에 맞게 이식했습니다.
 *
 * @param polylines    상태별 폴리라인 목록 (state: 0=이동, 1=탐색, 2=낚시)
 * @param stateMarkers 상태 변경 지점 마커 목록
 * @param photoMarkers 사진 마커 목록 (초록 테두리 썸네일)
 * @param onMarkerClick 마커 탭 콜백 — markerId를 반환
 * @param modifier     Composable 크기/위치 조정용
 */
@Composable
fun KakaoHistoryMapView(
    polylines: List<HistoryPolyline>,
    stateMarkers: List<HistoryStateMarker>,
    photoMarkers: List<HistoryPhotoMarker>,
    startMarker: HistoryEndpointMarker? = null,
    endMarker: HistoryEndpointMarker? = null,
    onMarkerClick: (markerId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // [개념] remember { }로 MapView 인스턴스를 재사용합니다.
    //        리컴포지션마다 새 MapView가 생성되면 지도가 초기화되고 성능이 저하됩니다.
    val mapView = remember { MapView(context) }

    // KakaoMap 인스턴스 — onMapReady에서 설정됩니다.
    // [개념] mutableStateOf로 선언하면 onMapReady에서 값이 설정될 때 Compose 리컴포지션이 트리거됩니다.
    //        이로 인해 LaunchedEffect(kakaoMap, ...)가 재실행되어 데이터를 지도에 그릴 수 있습니다.
    //        arrayOfNulls를 사용하면 Compose가 변경을 감지하지 못해 지도가 영구적으로 비어있는 버그가 발생합니다.
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }

    // [개념] DisposableEffect + LifecycleEventObserver로 Kakao MapView 생명주기를 연동합니다.
    //        iOS의 viewWillAppear/viewWillDisappear에서 engine.startRendering()/pauseRendering() 호출과 대응합니다.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.resume()
                Lifecycle.Event.ON_PAUSE  -> mapView.pause()
                // [개념] finish()는 GL 엔진을 완전히 종료합니다.
                //        Kakao SDK는 전역 GL 엔진을 공유하므로, finish()를 탭 전환 중 호출하면
                //        KakaoRecordMapView의 렌더링도 함께 중단되어 흰 화면이 됩니다.
                //        Activity가 실제로 소멸될 때(ON_DESTROY)만 finish()를 호출합니다.
                Lifecycle.Event.ON_DESTROY -> mapView.finish()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // [개념] popBackStack()으로 이 화면이 제거될 때 GL 레이어만 정리하고,
            //        엔진은 pause() 상태로 유지합니다. finish()는 ON_DESTROY에서만 호출합니다.
            kakaoMap?.let { map ->
                val sm = map.getShapeManager()
                val lm = map.getLabelManager()
                sm?.getLayer("history_poly_layer")?.let { sm.remove(it) }
                lm?.getLayer("history_state_layer")?.let { lm.remove(it) }
                lm?.getLayer("history_photo_layer")?.let { lm.remove(it) }
                lm?.getLayer("history_endpoint_layer")?.let { lm.remove(it) }
            }
            mapView.pause()
        }
    }

    // 지도 준비 완료 또는 데이터 로드 완료 시 경로/마커를 그립니다.
    // [개념] kakaoMap을 key에 포함하는 것이 핵심입니다.
    //        onMapReady에서 kakaoMap이 설정되면 리컴포지션이 발생하고 이 LaunchedEffect가 재실행됩니다.
    //        kakaoMap 없이 데이터 크기만 key로 사용하면:
    //          1) LaunchedEffect 첫 실행 시 kakaoMap == null → return
    //          2) 이후 onMapReady가 와도 LaunchedEffect가 재실행되지 않음 → 영구적으로 빈 지도
    LaunchedEffect(kakaoMap, polylines.size, stateMarkers.size, photoMarkers.size, startMarker, endMarker) {
        val map = kakaoMap ?: return@LaunchedEffect
        if (polylines.isEmpty() &&
            stateMarkers.isEmpty() &&
            photoMarkers.isEmpty() &&
            startMarker == null &&
            endMarker == null
        ) return@LaunchedEffect

        drawKakaoHistoryMap(
            kakaoMap = map,
            context = context,
            polylines = polylines,
            stateMarkers = stateMarkers,
            photoMarkers = photoMarkers,
            startMarker = startMarker,
            endMarker = endMarker,
            onMarkerClick = onMarkerClick
        )
        // 카메라 이동은 지도 렌더링 완료 후 실행합니다.
        // [개념] mapView.post { }는 현재 프레임 렌더링이 끝난 후 Runnable을 실행합니다.
        //        Google의 setOnMapLoadedCallback 패턴과 동일한 역할입니다.
        //        렌더링 전에 moveCamera를 호출하면 카카오 본사(기본 위치)에 고정되는 버그가 발생합니다.
        mapView.post {
            kakaoZoomToFit(map, polylines, stateMarkers, photoMarkers, startMarker, endMarker)
        }
    }

    // [개념] AndroidView의 factory는 최초 1회 View를 생성합니다.
    //        start()로 Kakao 엔진을 초기화하고, KakaoMapReadyCallback에서 KakaoMap 인스턴스를 받습니다.
    AndroidView(
        factory = { _ ->
            mapView.also { view ->
                view.start(
                    object : MapLifeCycleCallback() {
                        override fun onMapDestroy() {}
                        override fun onMapError(e: Exception) {}
                    },
                    object : KakaoMapReadyCallback() {
                        override fun onMapReady(map: KakaoMap) {
                            // [개념] mutableStateOf에 값을 설정하면 Compose 리컴포지션이 트리거됩니다.
                            //        이로 인해 LaunchedEffect(kakaoMap, ...)가 재실행되어
                            //        factory 클로저의 stale 데이터 문제 없이 현재 데이터로 지도를 그립니다.
                            kakaoMap = map
                        }
                    }
                )
            }
        },
        modifier = modifier
    )
}

// ── Kakao 지도 드로잉 ──────────────────────────────────────────────────────────

/**
 * Kakao Maps에 폴리라인과 마커를 그립니다.
 * iOS의 HistoryKakaoMapViewController.redrawAll()에 대응합니다.
 */
private fun drawKakaoHistoryMap(
    kakaoMap: KakaoMap,
    context: android.content.Context,
    polylines: List<HistoryPolyline>,
    stateMarkers: List<HistoryStateMarker>,
    photoMarkers: List<HistoryPhotoMarker>,
    startMarker: HistoryEndpointMarker?,
    endMarker: HistoryEndpointMarker?,
    onMarkerClick: (markerId: String) -> Unit
) {
    val shapeManager = kakaoMap.getShapeManager() ?: return
    val labelManager = kakaoMap.getLabelManager() ?: return

    // 기존 레이어를 제거하고 재생성합니다 (iOS의 clearAll → redrawAll 패턴).
    // [개념] 레이어를 매니저에서 통째로 제거하면 해당 레이어의 모든 드로잉이 초기화됩니다.
    //        shapeManager.remove(layer) / labelManager.remove(layer) API를 사용합니다.
    shapeManager.getLayer("history_poly_layer")?.let { shapeManager.remove(it) }
    labelManager.getLayer("history_state_layer")?.let { labelManager.remove(it) }
    labelManager.getLayer("history_photo_layer")?.let { labelManager.remove(it) }
    labelManager.getLayer("history_endpoint_layer")?.let { labelManager.remove(it) }

    // ── 1. 폴리라인 레이어 생성 및 그리기 ─────────────────────────────────
    // iOS의 HistoryPolylineLayer (zOrder: 10000)에 대응합니다.
    val polyLayer: ShapeLayer = shapeManager.addLayer(
        ShapeLayerOptions.from("history_poly_layer").setZOrder(10000)
    ) ?: return

    for (polyline in polylines) {
        if (polyline.coordinates.size < 2) continue

        val color = kakaoHistoryPolylineColor(polyline.state).toArgb()
        // [개념] MapPoints.fromLatLng()는 좌표 목록으로 폴리라인 포인트 집합을 생성합니다.
        //        PolylineOptions.from(MapPoints, PolylineStyle)로 스타일과 함께 폴리라인을 추가합니다.
        val points = polyline.coordinates.map { KakaoLatLng.from(it.latitude, it.longitude) }
        val mapPoints = MapPoints.fromLatLng(points)
        polyLayer.addPolyline(
            KakaoPolylineOptions.from(mapPoints, PolylineStyle.from(8f, color))
        )
    }

    // ── 2. 시작/종료 마커 레이어 생성 및 그리기 ──────────────────────────
    val endpointLayer: LabelLayer = labelManager.addLayer(
        LabelLayerOptions.from("history_endpoint_layer").setZOrder(14000)
    ) ?: return
    addKakaoHistoryEndpointLabel(
        context = context,
        layer = endpointLayer,
        marker = startMarker,
        iconResId = R.drawable.ic_map_marker_start,
        labelId = "history_start_marker"
    )
    addKakaoHistoryEndpointLabel(
        context = context,
        layer = endpointLayer,
        marker = endMarker,
        iconResId = R.drawable.ic_map_marker_end,
        labelId = "history_end_marker"
    )

    // ── 3. 상태 마커 레이어 생성 및 그리기 ────────────────────────────────
    // iOS의 HistoryStateLayer (zOrder: 15000)에 대응합니다.
    val stateLayer: LabelLayer = labelManager.addLayer(
        LabelLayerOptions.from("history_state_layer").setZOrder(15000)
    ) ?: return

    val density = context.resources.displayMetrics.density
    for (marker in stateMarkers) {
        val iconResId = when (marker.state) {
            FDAppManager.FishingState.MOVING   -> R.drawable.ic_map_marker_blue
            FDAppManager.FishingState.DRIFTING -> R.drawable.ic_map_marker_orange
            FDAppManager.FishingState.FISHING  -> R.drawable.ic_map_marker_red
        }
        val bitmap = BitmapFactory.decodeResource(context.resources, iconResId)
        // Kakao SDK는 비트맵 픽셀을 dp처럼 처리하므로, density로 나눠 Google Maps와 동일한 시각적 크기로 맞춥니다.
        val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width / density * 2).toInt(),
            (bitmap.height / density * 2).toInt(),
            true
        )
        // [개념] LabelOptions.from(id, LatLng)에서 첫 번째 파라미터가 레이블 ID입니다.
        //        setTag()로 클릭 이벤트 시 식별할 데이터를 추가로 저장합니다.
        val labelOptions = LabelOptions.from(marker.id, KakaoLatLng.from(marker.latitude, marker.longitude))
            .setStyles(LabelStyle.from(scaledBitmap))
            .setTag(marker.id)
        stateLayer.addLabel(labelOptions)
    }

    // ── 4. 사진 마커 레이어 생성 및 그리기 ────────────────────────────────
    // iOS의 HistoryPhotoLayer (zOrder: 20000)에 대응합니다.
    val photoLayer: LabelLayer = labelManager.addLayer(
        LabelLayerOptions.from("history_photo_layer").setZOrder(20000)
    ) ?: return

    for (marker in photoMarkers) {
        val thumbnailBitmap = loadHistoryPhotoMarkerBitmap(context, marker.thumbnailPath)
            ?: continue
        // [개념] LabelStyle.from(Bitmap)으로 이미지 마커를 생성합니다.
        //        LabelTextureStyle은 SDK 2.13.0에 존재하지 않습니다.
        val labelOptions = LabelOptions.from(marker.id, KakaoLatLng.from(marker.latitude, marker.longitude))
            .setStyles(LabelStyle.from(thumbnailBitmap))
            .setTag(marker.id)
        photoLayer.addLabel(labelOptions)
    }

    // ── 4. 레이블 탭 이벤트 등록 ──────────────────────────────────────────
    // [개념] setOnLabelClickListener로 레이블(마커) 클릭을 수신합니다.
    //        label.getTag()에 setTag()로 저장한 markerId가 반환됩니다.
    //        iOS의 addPoiTappedEventHandler에 대응합니다.
    kakaoMap.setOnLabelClickListener { _, _, label ->
        val markerId = label.getTag() as? String
        if (markerId != null) onMarkerClick(markerId)
        true
    }
}

/**
 * 전체 경로가 화면에 맞게 보이도록 카메라를 이동합니다.
 * iOS의 HistoryKakaoMapViewController.moveCameraToFit()에 대응합니다.
 */
private fun kakaoZoomToFit(
    kakaoMap: KakaoMap,
    polylines: List<HistoryPolyline>,
    stateMarkers: List<HistoryStateMarker>,
    photoMarkers: List<HistoryPhotoMarker>,
    startMarker: HistoryEndpointMarker?,
    endMarker: HistoryEndpointMarker?
) {
    val allCoords = mutableListOf<GeoCoord>()
    polylines.forEach { allCoords.addAll(it.coordinates) }
    stateMarkers.forEach { allCoords.add(GeoCoord(it.latitude, it.longitude)) }
    photoMarkers.forEach { allCoords.add(GeoCoord(it.latitude, it.longitude)) }
    startMarker?.let { allCoords.add(GeoCoord(it.latitude, it.longitude)) }
    endMarker?.let { allCoords.add(GeoCoord(it.latitude, it.longitude)) }

    if (allCoords.isEmpty()) return

    // 단일 좌표인 경우 고정 줌 레벨로 이동
    if (allCoords.size == 1) {
        kakaoMap.moveCamera(
            KakaoCameraUpdateFactory.newCenterPosition(
                KakaoLatLng.from(allCoords[0].latitude, allCoords[0].longitude), 15
            )
        )
        return
    }

    val minLat = allCoords.minOf { it.latitude }
    val maxLat = allCoords.maxOf { it.latitude }
    val minLon = allCoords.minOf { it.longitude }
    val maxLon = allCoords.maxOf { it.longitude }

    // iOS의 최소값 0.002 보장 로직에 대응합니다.
    val latDelta = maxOf(maxLat - minLat, 0.002)
    val lonDelta = maxOf(maxLon - minLon, 0.002)

    val centerLat = (minLat + maxLat) / 2.0
    val centerLon = (minLon + maxLon) / 2.0
    val zoomLevel = calculateZoomForBounds(latDelta, lonDelta)

    kakaoMap.moveCamera(
        KakaoCameraUpdateFactory.newCenterPosition(
            KakaoLatLng.from(centerLat, centerLon), zoomLevel
        )
    )
}

/**
 * 위도/경도 델타 범위에 따른 적절한 Kakao 줌 레벨을 반환합니다.
 * Kakao Maps의 줌 레벨은 1(가장 축소)~21(가장 확대)입니다.
 */
private fun calculateZoomForBounds(latDelta: Double, lonDelta: Double): Int {
    val maxDelta = maxOf(latDelta, lonDelta)
    return when {
        maxDelta < 0.001  -> 17
        maxDelta < 0.005  -> 16
        maxDelta < 0.01   -> 15
        maxDelta < 0.05   -> 14
        maxDelta < 0.1    -> 13
        maxDelta < 0.5    -> 11
        maxDelta < 1.0    -> 10
        else              -> 8
    }
}

/**
 * 폴리라인 state 값에 따른 색상을 반환합니다.
 * GoogleHistoryMapView의 polylineColor와 동일한 로직입니다.
 */
private fun kakaoHistoryPolylineColor(state: Int): Color = when (state) {
    0    -> KakaoHistoryMovingColor
    1    -> KakaoHistoryDriftingColor
    else -> KakaoHistoryFishingColor
}

private fun addKakaoHistoryEndpointLabel(
    context: android.content.Context,
    layer: LabelLayer,
    marker: HistoryEndpointMarker?,
    iconResId: Int,
    labelId: String
) {
    marker ?: return

    val bitmap = loadKakaoHistoryEndpointMarkerBitmap(context, iconResId)
    val labelOptions = LabelOptions.from(labelId, KakaoLatLng.from(marker.latitude, marker.longitude))
        .setStyles(LabelStyle.from(bitmap).setAnchorPoint(0.5f, 0.5f))
        .setTag(marker.id)
    layer.addLabel(labelOptions)
}

private fun loadKakaoHistoryEndpointMarkerBitmap(
    context: android.content.Context,
    iconResId: Int
): android.graphics.Bitmap {
    val bitmap = BitmapFactory.decodeResource(context.resources, iconResId)
    val density = context.resources.displayMetrics.density
    val width = (bitmap.width / density * 2).toInt().coerceAtLeast(1)
    val height = (bitmap.height / density * 2).toInt().coerceAtLeast(1)
    return android.graphics.Bitmap.createScaledBitmap(bitmap, width, height, true)
}

/**
 * 사진 마커 Bitmap 생성.
 * GoogleHistoryMapView의 loadHistoryPhotoMarkerBitmap과 동일한 로직입니다.
 * iOS의 generateThumbnail()에 대응합니다.
 */
private fun loadHistoryPhotoMarkerBitmap(
    context: android.content.Context,
    thumbnailPath: String
): android.graphics.Bitmap? {
    val thumbPath = thumbnailPath.removeSuffix(".jpg") + "_thumb.jpg"
    val file = java.io.File(context.filesDir, thumbPath).takeIf { it.exists() }
        ?: java.io.File(context.filesDir, thumbnailPath).takeIf { it.exists() }
        ?: return null

    val originalBitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath) ?: return null

    // Kakao SDK는 비트맵 픽셀을 dp처럼 처리하므로, density를 곱하지 않고 dp 값 그대로 사용합니다.
    // (Google Maps는 px→dp 변환을 SDK 내부에서 처리하지만, Kakao Maps는 px를 dp로 해석합니다.)
    val imageSize    = 96
    val borderWidth  = 6
    val cornerRadius = 20f
    val totalSize    = imageSize + borderWidth * 2

    val result = android.graphics.Bitmap.createBitmap(totalSize, totalSize, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(result)
    val paint  = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    // 1. 초록 테두리 배경 (Figma: #10B981)
    paint.color = android.graphics.Color.parseColor("#10B981")
    canvas.drawRoundRect(
        android.graphics.RectF(0f, 0f, totalSize.toFloat(), totalSize.toFloat()),
        cornerRadius + borderWidth,
        cornerRadius + borderWidth,
        paint
    )

    // 2. 이미지 영역 클리핑 후 Aspect Fill 방식으로 그리기
    val imageRect = android.graphics.RectF(
        borderWidth.toFloat(), borderWidth.toFloat(),
        (borderWidth + imageSize).toFloat(), (borderWidth + imageSize).toFloat()
    )
    val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(originalBitmap, imageSize, imageSize, true)
    canvas.save()
    val clipPath = android.graphics.Path().apply {
        addRoundRect(imageRect, cornerRadius, cornerRadius, android.graphics.Path.Direction.CW)
    }
    canvas.clipPath(clipPath)
    canvas.drawBitmap(scaledBitmap, borderWidth.toFloat(), borderWidth.toFloat(), null)
    canvas.restore()

    return result
}
