package com.onbada.seathermo.presentation.point.map_strategy

import androidx.fragment.app.FragmentManager
import com.onbada.seathermo.domain.entity.MapPin
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

class GoogleMapStrategy(
    private val fragmentManager: FragmentManager,
    private val containerId: Int
) : MapStrategy, OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener {

    private var googleMap: GoogleMap? = null
    private var onInitComplete: (() -> Unit)? = null
    
    // 마커 클릭 콜백을 저장하는 맵 (Marker -> Callback)
    private val markerClickCallbacks = mutableMapOf<Marker, (MapPin) -> Unit>()
    
    // 맵 빈 공간 클릭 콜백
    var onMapEmptyClick: (() -> Unit)? = null

    override fun initialize(onMapReady: () -> Unit) {
        this.onInitComplete = onMapReady
        
        val mapFragment = SupportMapFragment.newInstance()
        fragmentManager.beginTransaction()
            .replace(containerId, mapFragment)
            .commitNow() // 동기적으로 처리하여 getMapAsync 바로 호출 가능하도록 함
            
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        googleMap?.setOnMarkerClickListener(this)
        googleMap?.setOnMapClickListener(this)
        
        onInitComplete?.invoke()
    }

    override fun moveCamera(bounds: LatLngBounds, padding: Int) {
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
        googleMap?.moveCamera(cameraUpdate)
    }

    override fun moveCameraToPosition(latLng: LatLng, zoom: Float) {
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom)
        googleMap?.moveCamera(cameraUpdate)
    }

    override fun addPolyline(points: List<LatLng>, color: Int, width: Float) {
        val polylineOptions = PolylineOptions()
            .addAll(points)
            .color(color)
            .width(width)
        googleMap?.addPolyline(polylineOptions)
    }

    override fun addMarker(pin: MapPin, onClick: (MapPin) -> Unit) {
        val markerOptions = MarkerOptions()
            .position(LatLng(pin.latitude, pin.longitude))
            .title(pin.title)
            .snippet(pin.subtitle)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        
        val marker = googleMap?.addMarker(markerOptions) ?: return
        marker.tag = pin
        markerClickCallbacks[marker] = onClick
    }

    override fun clear() {
        googleMap?.clear()
        markerClickCallbacks.clear()
    }

    override fun setZoomControlsEnabled(enabled: Boolean) {
        googleMap?.uiSettings?.isZoomControlsEnabled = enabled
    }

    override fun setMyLocationEnabled(enabled: Boolean) {
        try {
            googleMap?.isMyLocationEnabled = enabled
        } catch (e: SecurityException) {
            android.util.Log.e("GoogleMapStrategy", "Location permission not granted", e)
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val callback = markerClickCallbacks[marker]
        if (callback != null) {
            val pin = marker.tag as? MapPin
            if (pin != null) {
                callback(pin)
                return true
            }
        }
        return false
    }

    override fun onMapClick(latLng: LatLng) {
        onMapEmptyClick?.invoke()
    }
}
