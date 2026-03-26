package com.onbada.seathermo.presentation.history.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.onbada.seathermo.R
import com.onbada.seathermo.managers.FDAppManager
import com.onbada.seathermo.presentation.history.viewmodel.GeoCoord
import com.onbada.seathermo.presentation.history.viewmodel.HistoryPhotoMarker
import com.onbada.seathermo.presentation.history.viewmodel.HistoryPolyline
import com.onbada.seathermo.presentation.history.viewmodel.HistoryStateMarker
import java.io.File

// ── 폴리라인 색상 (FishingRecordScreen과 동일) ─────────────────────────────────
private val StateMovingColor   = Color(0xFF2563EB)  // 이동: 파랑
private val StateDriftingColor = Color(0xFFF59E0B)  // 탐색: 주황
private val StateFishingColor  = Color(0xFFEF4444)  // 낚시: 빨강

/**
 * 히스토리 상세 화면용 Google Maps 컴포저블.
 *
 * [개념] FishingRecordScreen의 GoogleRecordMapView와 달리,
 *        이 컴포넌트는 이미 저장된 경로(polylines)와 마커(markers)를 한 번에 그립니다.
 *        실시간 추가가 아닌 전체 데이터를 최초 1회 렌더링하는 방식입니다.
 *        iOS의 HistoryMapView (UIViewRepresentable) 로직에 대응합니다.
 *
 * @param polylines    상태별 폴리라인 목록 (state: 0=이동, 1=탐색, 2=낚시)
 * @param stateMarkers 상태 변경 지점 마커 목록
 * @param photoMarkers 사진 마커 목록 (초록 테두리 썸네일)
 * @param onMarkerClick 마커 탭 콜백 — tag로 전달된 markerId를 반환
 * @param modifier     Composable 크기/위치 조정용
 */
@Composable
fun GoogleHistoryMapView(
    polylines: List<HistoryPolyline>,
    stateMarkers: List<HistoryStateMarker>,
    photoMarkers: List<HistoryPhotoMarker>,
    onMarkerClick: (markerId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // [개념] remember { }로 MapView 인스턴스를 재사용합니다.
    //        리컴포지션마다 새 MapView가 생성되면 지도가 초기화되고 성능이 저하됩니다.
    val mapView = remember { MapView(context) }

    // [개념] DisposableEffect + LifecycleEventObserver로 MapView 생명주기를 연동합니다.
    //        iOS의 UIViewRepresentable의 makeUIView/updateUIView와 달리
    //        Android는 생명주기 메서드를 수동으로 연결해야 합니다.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE  -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START   -> mapView.onStart()
                Lifecycle.Event.ON_RESUME  -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE   -> mapView.onPause()
                Lifecycle.Event.ON_STOP    -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 지도 준비 후 데이터를 한 번에 렌더링합니다.
    // [개념] LaunchedEffect의 key에 polylines.size를 사용하면 데이터 로드 완료 시 재실행됩니다.
    //        초기에 빈 리스트로 진입했다가 fetchSessionData() 완료 후 데이터가 채워지는 흐름입니다.
    LaunchedEffect(polylines.size, stateMarkers.size, photoMarkers.size) {
        mapView.getMapAsync { googleMap ->
            // 지도 UI 설정
            googleMap.uiSettings.apply {
                isZoomControlsEnabled = false
                isMyLocationButtonEnabled = false
                isCompassEnabled = false
            }

            // 마커 클릭 리스너 등록
            // [개념] Marker.tag에 저장한 markerId로 어떤 마커가 탭됐는지 식별합니다.
            //        iOS의 mapView(_:didSelect:) → annotationId 매핑 패턴에 대응합니다.
            googleMap.setOnMarkerClickListener { marker ->
                val markerId = marker.tag as? String ?: return@setOnMarkerClickListener false
                onMarkerClick(markerId)
                true // 기본 info window 표시 억제
            }

            // 데이터가 있을 때만 지도를 채웁니다.
            if (polylines.isNotEmpty() || stateMarkers.isNotEmpty() || photoMarkers.isNotEmpty()) {
                drawHistoryMap(googleMap, context, polylines, stateMarkers, photoMarkers)
                // [개념] setOnMapLoadedCallback은 지도 타일과 View 레이아웃이 모두 완료된 후 1회 실행됩니다.
                //        mapView.post보다 확실하게 완료 시점을 보장합니다.
                //        newLatLngBounds는 MapView의 실제 크기(width/height)를 알아야 동작하므로
                //        레이아웃 완료 전 호출하면 IllegalStateException이 발생해 고정 줌으로 폴백됩니다.
                googleMap.setOnMapLoadedCallback {
                    zoomToFit(googleMap, polylines, stateMarkers, photoMarkers)
                }
            }
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}

// ── 지도 드로잉 ────────────────────────────────────────────────────────────────

/**
 * 폴리라인과 마커를 지도에 그립니다.
 * iOS의 updatePolyline + updateAnnotations에 대응합니다.
 */
private fun drawHistoryMap(
    googleMap: GoogleMap,
    context: Context,
    polylines: List<HistoryPolyline>,
    stateMarkers: List<HistoryStateMarker>,
    photoMarkers: List<HistoryPhotoMarker>
) {
    googleMap.clear()

    // 1. 폴리라인 (상태별 색상)
    for (polyline in polylines) {
        if (polyline.coordinates.size < 2) continue
        val color = polylineColor(polyline.state)
        googleMap.addPolyline(
            PolylineOptions()
                .addAll(polyline.coordinates.map { LatLng(it.latitude, it.longitude) })
                .width(8f)
                .color(color.toArgb())
                .geodesic(false)
        )
    }

    // 2. 상태 마커 (ic_map_marker_blue/orange/red)
    for (marker in stateMarkers) {
        val iconResId = when (marker.state) {
            FDAppManager.FishingState.MOVING   -> R.drawable.ic_map_marker_blue
            FDAppManager.FishingState.DRIFTING -> R.drawable.ic_map_marker_orange
            FDAppManager.FishingState.FISHING  -> R.drawable.ic_map_marker_red
        }
        val bitmap = BitmapFactory.decodeResource(context.resources, iconResId)
        googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(marker.latitude, marker.longitude))
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                .anchor(0.5f, 1.0f) // 마커 하단 중앙이 좌표에 위치
        )?.also { it.tag = marker.id }  // id를 tag에 저장 → 클릭 시 식별
    }

    // 3. 사진 마커 (초록 테두리 썸네일)
    for (marker in photoMarkers) {
        val thumbnailBitmap = loadHistoryPhotoMarkerBitmap(context, marker.thumbnailPath)
        val icon = thumbnailBitmap?.let { BitmapDescriptorFactory.fromBitmap(it) }
            ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
        googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(marker.latitude, marker.longitude))
                .icon(icon)
                .anchor(0.5f, 1.0f)
        )?.also { it.tag = marker.id }  // id를 tag에 저장
    }
}

/**
 * 전체 경로가 화면에 딱 맞게 보이도록 카메라를 이동합니다. (iOS의 Zoom to Fit에 대응)
 * iOS의 regionFor(coordinates:) + setRegion 패턴에 대응합니다.
 */
private fun zoomToFit(
    googleMap: GoogleMap,
    polylines: List<HistoryPolyline>,
    stateMarkers: List<HistoryStateMarker>,
    photoMarkers: List<HistoryPhotoMarker>
) {
    val allCoords = mutableListOf<GeoCoord>()
    polylines.forEach { allCoords.addAll(it.coordinates) }
    stateMarkers.forEach { allCoords.add(GeoCoord(it.latitude, it.longitude)) }
    photoMarkers.forEach { allCoords.add(GeoCoord(it.latitude, it.longitude)) }

    if (allCoords.isEmpty()) return

    // 단일 좌표인 경우 고정 줌 레벨로 이동
    if (allCoords.size == 1) {
        googleMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(allCoords[0].latitude, allCoords[0].longitude), 15f
            )
        )
        return
    }

    // 전체 좌표가 들어오는 LatLngBounds 생성 후 패딩 적용
    val boundsBuilder = LatLngBounds.builder()
    allCoords.forEach { boundsBuilder.include(LatLng(it.latitude, it.longitude)) }

    // [개념] newLatLngBoundsWithSize 대신 newLatLngBounds를 사용하면 지도 크기가 확정되지 않은
    //        시점에 IllegalStateException이 발생할 수 있습니다.
    //        post { } 방식으로 View 크기 확정 후 카메라를 이동합니다.
    try {
        googleMap.moveCamera(
            CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 300)
        )
    } catch (e: Exception) {
        // 지도 View가 아직 측정되지 않은 경우 중심 좌표로 폴백
        val center = allCoords[allCoords.size / 2]
        googleMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(center.latitude, center.longitude), 13f)
        )
    }
}

// ── 헬퍼 함수들 ───────────────────────────────────────────────────────────────

/**
 * 폴리라인 상태값(Int)에 따른 색상을 반환합니다.
 * iOS의 lineColor 프로퍼티에 대응합니다.
 */
private fun polylineColor(state: Int): Color = when (state) {
    0    -> StateMovingColor
    1    -> StateDriftingColor
    else -> StateFishingColor
}

/**
 * 사진 마커용 비트맵 생성.
 * FishingRecordScreen의 loadPhotoMarkerBitmap과 동일한 로직입니다.
 * iOS의 photoAnnotationView 내 썸네일 렌더링에 대응합니다.
 *
 * @param thumbnailPath filesDir 기준 상대 경로
 */
private fun loadHistoryPhotoMarkerBitmap(context: Context, thumbnailPath: String): Bitmap? {
    // 썸네일(uuid_thumb.jpg)을 우선 로드합니다. 없으면 원본(uuid.jpg)으로 폴백합니다.
    // [이유] 기존 레코드(업데이트 이전)에는 uuid_thumb.jpg가 없으므로 폴백이 필요합니다.
    val thumbPath = thumbnailPath.removeSuffix(".jpg") + "_thumb.jpg"
    val file = File(context.filesDir, thumbPath).takeIf { it.exists() }
        ?: File(context.filesDir, thumbnailPath).takeIf { it.exists() }
        ?: return null

    val originalBitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null

    val density = context.resources.displayMetrics.density
    val imageSize    = (48 * density).toInt()
    val borderWidth  = (3 * density).toInt()
    val cornerRadius = 10f * density
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
    val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, imageSize, imageSize, true)
    canvas.save()
    val clipPath = android.graphics.Path().apply {
        addRoundRect(imageRect, cornerRadius, cornerRadius, android.graphics.Path.Direction.CW)
    }
    canvas.clipPath(clipPath)
    canvas.drawBitmap(scaledBitmap, borderWidth.toFloat(), borderWidth.toFloat(), null)
    canvas.restore()

    return result
}
