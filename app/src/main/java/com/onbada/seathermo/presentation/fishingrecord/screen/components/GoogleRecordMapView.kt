package com.onbada.seathermo.presentation.fishingrecord.screen.components

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView

/**
 * Google Maps를 Compose AndroidView로 래핑한 지도 컴포넌트.
 *
 * [개념] AndroidView { }는 기존 View 시스템 컴포넌트를 Compose에 통합할 때 사용합니다.
 *        iOS의 UIViewRepresentable 프로토콜에 대응합니다.
 *
 * [개념] Google Maps의 MapView는 Activity/Fragment의 생명주기 메서드를 직접 호출해야 합니다.
 *        DisposableEffect + LifecycleEventObserver로 Composable 생명주기와 연동합니다.
 *        iOS의 UIViewRepresentable의 makeUIView/updateUIView와 달리,
 *        Android에서는 생명주기를 수동으로 연결해야 합니다.
 *
 * @param modifier Composable 크기/위치 조정용 Modifier
 * @param onMapReady GoogleMap 인스턴스가 준비되면 호출되는 콜백. 지도 초기 설정에 사용합니다.
 */
@Composable
fun GoogleRecordMapView(
    modifier: Modifier = Modifier,
    onMapReady: (GoogleMap) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // [개념] remember { }는 리컴포지션이 발생해도 동일한 인스턴스를 재사용합니다.
    //        MapView는 한 번만 생성해야 하므로 remember로 보관합니다.
    //        Swift의 @State private var mapView = MapView()와 동일한 의도입니다.
    val mapView = remember { MapView(context) }

    // [개념] DisposableEffect는 Composable이 컴포지션에 진입/퇴장할 때 작업을 수행합니다.
    //        LifecycleEventObserver로 호스트 생명주기 이벤트를 수신하여
    //        MapView의 생명주기 메서드(onCreate/onStart/onResume 등)를 호출합니다.
    //        onDispose에서 Observer를 해제하여 메모리 누수를 방지합니다.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // [개념] AndroidView의 factory는 최초 1회 View를 생성합니다.
    //        update 블록은 상태가 변경될 때마다 호출됩니다.
    //        getMapAsync는 매번 호출해도 준비된 동일한 GoogleMap 인스턴스를 콜백으로 반환합니다.
    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            view.getMapAsync { googleMap ->
                onMapReady(googleMap)
            }
        }
    )
}
