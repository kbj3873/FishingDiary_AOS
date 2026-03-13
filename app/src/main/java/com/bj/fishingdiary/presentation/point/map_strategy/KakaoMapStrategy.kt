package com.bj.fishingdiary.presentation.point.map_strategy

import android.content.Context
import android.view.ViewGroup
import com.bj.fishingdiary.domain.entity.MapPin
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng as KakaoLatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelLayer
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelManager
import com.kakao.vectormap.label.LabelTextBuilder
import com.kakao.vectormap.route.RouteLine
import com.kakao.vectormap.route.RouteLineLayer
import com.kakao.vectormap.route.RouteLineOptions
import com.kakao.vectormap.route.RouteLineSegment
import com.kakao.vectormap.route.RouteLineStyle
import com.kakao.vectormap.route.RouteLineManager

class KakaoMapStrategy(
    private val context: Context,
    private val container: ViewGroup // 지도를 추가할 부모 뷰
) : MapStrategy {

    private var kakaoMap: KakaoMap? = null
    private var mapView: MapView? = null
    private var onInitComplete: (() -> Unit)? = null

    // 레이어 저장
    private var routeLineLayer: RouteLineLayer? = null
    private var labelLayer: LabelLayer? = null

    // 마커 클릭 콜백 (Label -> Callback)
    private val labelClickCallbacks = mutableMapOf<Label, (MapPin) -> Unit>()

    // 맵 빈 공간 클릭 콜백
    var onMapEmptyClick: (() -> Unit)? = null

    // 현재 위치 마커
    private var myLocationLabel: Label? = null
    private var isMyLocationEnabled = false
    private var pendingLocation: LatLng? = null

    override fun initialize(onMapReady: () -> Unit) {
        android.util.Log.d("Kakao Map Debug", "KakaoMapStrategy initialize called")
        this.onInitComplete = onMapReady

        // MapView 생성 및 추가
        mapView = MapView(context)
        container.removeAllViews()
        container.addView(mapView, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        mapView?.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {
                // 지도 종료 시 처리
            }

            override fun onMapError(error: Exception?) {
                android.util.Log.e("Kakao Map Debug", "Kakao Map Error: ${error?.message}")
            }
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                android.util.Log.d("Kakao Map Debug", "Kakao Map Ready")
                kakaoMap = map

                // 레이어 초기화
                initializeLayers(map)

                setupMapListeners()
                
                // 대기 중인 위치 업데이트가 있으면 처리
                pendingLocation?.let {
                    android.util.Log.d("Kakao Map Debug", "Processing pending location update")
                    updateMyLocation(it)
                    pendingLocation = null
                }
                
                onInitComplete?.invoke()
            }
        })
    }

    private fun initializeLayers(map: KakaoMap) {
        android.util.Log.d("Kakao Map Debug", "Initializing layers")

        // RouteLineLayer 초기화
        routeLineLayer = map.routeLineManager?.layer
        if (routeLineLayer == null) {
            android.util.Log.d("Kakao Map Debug", "Default RouteLineLayer is null, creating custom layer")
            routeLineLayer = map.routeLineManager?.addLayer("routeLayer", 10000)
        }
        android.util.Log.d("Kakao Map Debug", "RouteLineLayer: $routeLineLayer")

        // LabelLayer 초기화
        labelLayer = map.labelManager?.layer
        if (labelLayer == null) {
            android.util.Log.d("Kakao Map Debug", "Default LabelLayer is null, creating custom layer")
            labelLayer = map.labelManager?.addLayer(
                com.kakao.vectormap.label.LabelLayerOptions.from("labelLayer")
            )
        }
        android.util.Log.d("Kakao Map Debug", "LabelLayer: $labelLayer")
    }

    private fun setupMapListeners() {
        kakaoMap?.setOnLabelClickListener { _, _, label ->
            // Label 클릭 시 처리
            val callback = labelClickCallbacks[label]
            if (callback != null) {
                val pin = label.tag as? MapPin
                if (pin != null) {
                    callback(pin)
                    return@setOnLabelClickListener true // 이벤트 소비
                }
            }
            return@setOnLabelClickListener false
        }
        
        kakaoMap?.setOnMapClickListener { _, _, _, _ ->
            onMapEmptyClick?.invoke()
        }
    }

    override fun moveCamera(bounds: LatLngBounds, padding: Int) {
        val sw = KakaoLatLng.from(bounds.southwest.latitude, bounds.southwest.longitude)
        val ne = KakaoLatLng.from(bounds.northeast.latitude, bounds.northeast.longitude)

        // LatLngBounds의 4개 코너를 LatLng 배열로 변환
        val points = arrayOf(sw, ne)

        // Kakao Map에서 points에 맞게 카메라 이동 (fitMapPoints 사용)
        val cameraUpdate = CameraUpdateFactory.fitMapPoints(points, padding)
        kakaoMap?.moveCamera(cameraUpdate)
        android.util.Log.d("Kakao Map Debug", "Camera moved to fit points with padding: $padding")
    }

    override fun moveCameraToPosition(latLng: LatLng, zoom: Float) {
        val kakaoLatLng = KakaoLatLng.from(latLng.latitude, latLng.longitude)

        // 1. 먼저 위치로 이동
        val positionUpdate = CameraUpdateFactory.newCenterPosition(kakaoLatLng)
        kakaoMap?.moveCamera(positionUpdate)

        // 2. 그 다음 줌 레벨 설정
        val zoomUpdate = CameraUpdateFactory.zoomTo(zoom.toInt())
        kakaoMap?.moveCamera(zoomUpdate)

        android.util.Log.d("Kakao Map Debug", "Camera moved to position: ${latLng.latitude}, ${latLng.longitude} with zoom: $zoom")
    }

    override fun addPolyline(points: List<LatLng>, color: Int, width: Float) {
        if (points.isEmpty()) return
        android.util.Log.d("Kakao Map Debug", "Strategy addPolyline: ${points.size} points, color=$color")

        if (routeLineLayer == null) {
            android.util.Log.e("Kakao Map Debug", "RouteLineLayer is null, cannot add polyline")
            return
        }

        val kakaoPoints = points.map { KakaoLatLng.from(it.latitude, it.longitude) }

        // 두께를 좀 더 키워서(20f) 확실히 보이게 함
        val styles = RouteLineStyle.from(width, color)

        val segment = RouteLineSegment.from(kakaoPoints, styles)
        val options = RouteLineOptions.from(segment)

        // layer를 통해 RouteLine 추가
        val routeLine = routeLineLayer?.addRouteLine(options)
        android.util.Log.d("Kakao Map Debug", "RouteLine added? ${routeLine != null}, routeLine=$routeLine")
    }

    override fun addMarker(pin: MapPin, onClick: (MapPin) -> Unit) {
        val pos = KakaoLatLng.from(pin.latitude, pin.longitude)
        android.util.Log.d("Kakao Map Debug", "Adding marker at (${pin.latitude}, ${pin.longitude})")

        if (labelLayer == null) {
            android.util.Log.e("Kakao Map Debug", "LabelLayer is null, cannot add marker")
            return
        }

        // 포인트 마커 스타일 (아이콘만 표시, 텍스트 없음)
        val styles = com.kakao.vectormap.label.LabelStyles.from(
            com.kakao.vectormap.label.LabelStyle.from(com.bj.fishingdiary.R.drawable.ic_point)
        )

        val options = LabelOptions.from(pos)
            .setStyles(styles)
            .setTag(pin)

        // layer를 통해 Label 추가
        val label = labelLayer?.addLabel(options)
        android.util.Log.d("Kakao Map Debug", "Label added? ${label != null}, label=$label")

        if (label == null) {
            android.util.Log.e("Kakao Map Debug", "Failed to add label!")
            return
        }
        labelClickCallbacks[label] = onClick
    }

    override fun clear() {
        android.util.Log.d("Kakao Map Debug", "clear() called")
        routeLineLayer?.removeAll()
        labelLayer?.removeAll()
        labelClickCallbacks.clear()
    }

    override fun setZoomControlsEnabled(enabled: Boolean) {
        // Kakao Map V2 does not have built-in zoom controls in standard UI settings like Google
        // You might need to add custom buttons or assume default gestures are enough.
    }

    override fun setMyLocationEnabled(enabled: Boolean) {
        isMyLocationEnabled = enabled
        if (!enabled) {
            // 현재 위치 마커 제거
            myLocationLabel?.let {
                labelLayer?.remove(it)
                myLocationLabel = null
            }
        }
        android.util.Log.d("Kakao Map Debug", "My location enabled: $enabled")
    }

    /**
     * 현재 위치 업데이트 (Kakao Map용 커스텀 구현)
     * TrackMapActivity에서 호출하여 현재 위치 마커를 업데이트
     */
    fun updateMyLocation(latLng: LatLng) {
        android.util.Log.d("Kakao Map Debug", "updateMyLocation called: enabled=$isMyLocationEnabled, lat=${latLng.latitude}, lng=${latLng.longitude}")

        if (!isMyLocationEnabled) {
            android.util.Log.d("Kakao Map Debug", "My location is disabled, skipping marker update")
            return
        }

        if (labelLayer == null) {
            android.util.Log.w("Kakao Map Debug", "LabelLayer is null, saving location to pending")
            pendingLocation = latLng
            return
        }

        val kakaoLatLng = KakaoLatLng.from(latLng.latitude, latLng.longitude)

        // 기존 마커가 있으면 제거
        myLocationLabel?.let {
            android.util.Log.d("Kakao Map Debug", "Removing previous location marker")
            labelLayer?.remove(it)
            myLocationLabel = null
        }

        // 새로운 현재 위치 마커 추가 (ic_my_location 사용)
        val styles = com.kakao.vectormap.label.LabelStyles.from(
            com.kakao.vectormap.label.LabelStyle.from(com.bj.fishingdiary.R.drawable.ic_my_location)
                .setAnchorPoint(0.5f, 0.5f) // 이미지 중앙을 좌표에 맞춤
        )

        val options = LabelOptions.from(kakaoLatLng)
            .setStyles(styles)
            .setRank(10000) // 다른 마커보다 위에 표시

        myLocationLabel = labelLayer?.addLabel(options)
        android.util.Log.d("Kakao Map Debug", "My location marker added: ${myLocationLabel != null}, at ${latLng.latitude}, ${latLng.longitude}")
    }
}
