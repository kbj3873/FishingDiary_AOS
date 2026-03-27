package com.onbada.seathermo.presentation.fishingrecord.screen.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView

/**
 * Kakao Maps를 Compose AndroidView로 래핑한 낚시 기록용 지도 컴포넌트.
 *
 * [개념] AndroidView { }는 기존 View 시스템 컴포넌트를 Compose에 통합할 때 사용합니다.
 *        iOS의 UIViewControllerRepresentable 프로토콜에 대응합니다.
 *
 * [개념] Kakao MapView는 Google MapView와 달리 start() 메서드로 초기화하고,
 *        MapLifeCycleCallback + KakaoMapReadyCallback 두 개의 콜백으로 라이프사이클을 관리합니다.
 *        iOS의 viewDidLoad() + authenticationSucceeded() 패턴에 대응합니다.
 *
 * 실제 경로선/마커 드로잉 로직은 FishingRecordScreen의 LaunchedEffect 블록에서 처리합니다.
 * GoogleRecordMapView와 동일한 인터페이스를 제공하여 mapType 분기 전환을 단순하게 유지합니다.
 *
 * @param modifier Composable 크기/위치 조정용 Modifier
 * @param onMapReady KakaoMap 인스턴스가 준비되면 호출되는 콜백. 레이어 초기화에 사용합니다.
 * @param onMapError 지도 초기화 오류 발생 시 호출되는 콜백
 */
@Composable
fun KakaoRecordMapView(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    onMapReady: (KakaoMap) -> Unit = {},
    onMapError: (Exception) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // [개념] remember { }는 리컴포지션이 발생해도 동일한 인스턴스를 재사용합니다.
    //        MapView는 한 번만 생성해야 하므로 remember로 보관합니다.
    //        Swift의 KMViewContainer를 viewDidLoad에서 한 번만 생성하는 패턴에 대응합니다.
    val mapView = remember { MapView(context) }

    // [개념] rememberUpdatedState는 항상 최신 값을 참조하는 State를 만듭니다.
    //        DisposableEffect 안의 람다(observer)가 isVisible의 최신 값을 캡처할 수 있도록 합니다.
    //        isVisible을 DisposableEffect의 key로 쓰면 isVisible이 바뀔 때마다 onDispose가 실행되어
    //        mapView.finish()가 탭 전환마다 호출되는 버그가 발생합니다.
    val currentIsVisible by rememberUpdatedState(isVisible)

    // 탭 전환 시 MapView 렌더링을 제어합니다.
    // [개념] Kakao SDK는 전역 GL 엔진을 공유하므로, 탭이 비활성화될 때 pause()로 렌더링을 중단해야
    //        KakaoHistoryMapView와 카메라/레이어가 충돌하지 않습니다.
    //        iOS의 viewWillAppear → startRendering(), viewWillDisappear → stopRendering() 패턴에 대응합니다.
    LaunchedEffect(isVisible) {
        if (isVisible) mapView.resume() else mapView.pause()
    }

    // [개념] DisposableEffect + LifecycleEventObserver로 앱 백그라운드/포그라운드 생명주기를 처리합니다.
    //        key를 lifecycleOwner만으로 제한하여 onDispose(mapView.finish())가 탭 전환 시 호출되지 않도록 합니다.
    //        ON_RESUME 시 currentIsVisible로 비활성 탭의 MapView가 의도치 않게 resume되지 않도록 합니다.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (currentIsVisible) mapView.resume()
                Lifecycle.Event.ON_PAUSE -> mapView.pause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // [개념] MapView.finish()는 Kakao 맵 엔진 렌더링을 완전히 중단하고 리소스를 해제합니다.
            //        DisposableEffect key가 lifecycleOwner뿐이므로 Activity가 실제로 소멸될 때만 호출됩니다.
            //        iOS의 deinit { engine.stopEngine() }에 대응합니다.
            mapView.finish()
        }
    }

    // [개념] AndroidView의 factory는 최초 1회 View를 생성합니다.
    //        Kakao MapView는 start() 호출로 엔진을 초기화하고,
    //        KakaoMapReadyCallback.onMapReady()에서 KakaoMap 인스턴스를 제공합니다.
    //        iOS의 KMController.prepareEngine() + authenticationSucceeded() 콜백 흐름과 동일합니다.
    AndroidView(
        factory = { _ ->
            mapView.also { view ->
                view.start(
                    // MapLifeCycleCallback: 지도 라이프사이클 이벤트 처리
                    // iOS의 MapLifeCycleCallback에 대응합니다.
                    object : MapLifeCycleCallback() {
                        override fun onMapDestroy() {
                            // 지도 정리 완료 — 추가 처리 불필요
                        }
                        override fun onMapError(error: Exception) {
                            onMapError(error)
                        }
                    },
                    // KakaoMapReadyCallback: 지도 준비 완료 후 KakaoMap 인스턴스 전달
                    // iOS의 authenticationSucceeded() + addViews()에 대응합니다.
                    object : KakaoMapReadyCallback() {
                        override fun onMapReady(kakaoMap: KakaoMap) {
                            onMapReady(kakaoMap)
                        }
                    }
                )
            }
        },
        modifier = modifier
    )
}
