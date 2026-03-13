package com.bj.fishingdiary.presentation.point.map_strategy

import com.bj.fishingdiary.domain.entity.MapPin
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

/**
 * 지도 전략 인터페이스
 * Map Strategy Interface
 * 
 * Google Maps와 Kakao Maps를 동일한 방식으로 제어하기 위한 추상화
 */
interface MapStrategy {
    /**
     * 지도 초기화 (비동기)
     */
    fun initialize(onMapReady: () -> Unit)

    /**
     * 카메라 이동 (영역 기준)
     */
    fun moveCamera(bounds: LatLngBounds, padding: Int)

    /**
     * 카메라 이동 (위치 + 줌 레벨 기준)
     */
    fun moveCameraToPosition(latLng: LatLng, zoom: Float)

    /**
     * 폴리라인(경로) 추가
     */
    fun addPolyline(points: List<LatLng>, color: Int, width: Float)

    /**
     * 마커 추가
     */
    fun addMarker(pin: MapPin, onClick: (MapPin) -> Unit)

    /**
     * 지도 클리어 (모든 오버레이 제거)
     */
    fun clear()

    /**
     * 줌 컨트롤 활성화 여부
     */
    fun setZoomControlsEnabled(enabled: Boolean)

    /**
     * 현재 위치 표시 활성화 여부
     */
    fun setMyLocationEnabled(enabled: Boolean)
}
