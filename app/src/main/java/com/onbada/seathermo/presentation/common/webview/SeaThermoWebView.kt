package com.onbada.seathermo.presentation.common.webview

import android.annotation.SuppressLint
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.onbada.seathermo.BuildConfig

private const val TAG = "SeaThermoWebView"

/**
 * 온바다 서버 웹 콘텐츠 표시를 위한 공통 WebView Composable.
 *
 * [개념] Compose 환경에서 기존 Android View를 사용할 때는 AndroidView { }를 통해 래핑합니다.
 *        iOS의 SeaThermoWebView.swift (UIViewRepresentable)에 대응합니다.
 *
 * 주요 기능:
 * - 커스텀 User-Agent (SeaThermo 식별자 포함)
 * - 앱/디바이스 정보 헤더 (X-App-Version, X-OS-Name 등)
 * - JS Bridge: 웹 페이지에서 close 메시지 수신 시 뒤로가기 실행
 *   iOS: window.webkit.messageHandlers.appBridge.postMessage("close")
 *   Android: 동일한 인터페이스를 JavaScript 심(shim)으로 제공
 *
 * @param url     로드할 전체 URL
 * @param onNavigateBack 뒤로가기 콜백 (JS Bridge "close" 또는 시스템 뒤로가기)
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SeaThermoWebView(
    url: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    Log.d(TAG, "SeaThermoWebView Loading: $url")

    // [개념] remember { }는 Recomposition이 일어나도 WebView 인스턴스를 재생성하지 않습니다.
    //        AndroidView의 factory 블록에서 생성한 View를 캐싱하는 역할을 합니다.
    val webView = remember {
        WebView(context).apply {

            // ── 1. JavaScript 활성화 ─────────────────────────────────
            // [개념] 기본적으로 WebView의 JavaScript는 비활성화되어 있습니다.
            //        웹 페이지의 JS Bridge를 사용하려면 반드시 활성화해야 합니다.
            settings.javaScriptEnabled = true

            // DOM Storage 활성화 (웹 앱 localStorage 지원)
            settings.domStorageEnabled = true

            // ── 2. 커스텀 User-Agent ──────────────────────────────────
            // iOS: "SeaThermo/{version} (iPhone; iOS {osVersion})"
            // Android: "SeaThermo/{version} (Android; Android {osVersion})"
            val appVersion = BuildConfig.VERSION_NAME
            val osVersion = Build.VERSION.RELEASE
            settings.userAgentString = "SeaThermo/$appVersion (Android; Android $osVersion)"

            // ── 3. JS Bridge (appBridge) ──────────────────────────────
            // iOS: window.webkit.messageHandlers.appBridge.postMessage("close")
            // Android: window.appBridge.postMessage("close") (새 권장 방식)
            //          window.Android.postMessage("close") (기존 호환성용)
            //
            // 서버 웹 페이지가 iOS 방식 또는 새 Android 브릿지 방식(window.appBridge)으로
            // 호출하도록 설계되어 있으므로, 인터페이스를 등록합니다.
            // [개념] addJavascriptInterface()로 Kotlin 객체를 JavaScript에서 접근 가능하게 합니다.
            //        @JavascriptInterface 어노테이션이 붙은 메서드만 JS에서 호출할 수 있습니다.
            val bridgeInterface = AppBridgeInterface(onClose = {
                Log.d(TAG, "Bridge: Closing Web Page")
                // [개념] JS는 백그라운드 스레드에서 실행될 수 있으므로
                //        메인 스레드(UI 스레드)에서 네비게이션 처리가 필요합니다.
                //        iOS의 Task { @MainActor in self.onPop() }에 대응합니다.
                Handler(Looper.getMainLooper()).post { 
                    Log.d(TAG, "Bridge: Executing onNavigateBack")
                    onNavigateBack() 
                }
            })

            // 가이드에 따른 'appBridge' 등록 (window.appBridge.postMessage 호출 대응)
            addJavascriptInterface(bridgeInterface, "appBridge")

            // 기존 'Android' 인터페이스 유지 (하위 호환성)
            addJavascriptInterface(bridgeInterface, "Android")

            // ── 4. WebViewClient ──────────────────────────────────────
            // [개념] WebViewClient는 WebView의 페이지 로드 이벤트를 처리합니다.
            //        iOS의 WKNavigationDelegate에 대응합니다.
            webViewClient = object : WebViewClient() {

                // 페이지 로드 완료 시 iOS webkit 브릿지 심(shim) 주입
                // [개념] evaluateJavascript()로 페이지 로드 후 JavaScript를 직접 실행합니다.
                //        window.webkit.messageHandlers.appBridge를 Android 브릿지로 연결합니다.
                override fun onPageFinished(view: WebView?, pageUrl: String?) {
                    super.onPageFinished(view, pageUrl)
                    Log.d(TAG, "Page Finished: $pageUrl")
                    view?.evaluateJavascript(WEBKIT_BRIDGE_SHIM, null)
                }

                // 로드 오류 처리
                // iOS의 webView(_:didFailProvisionalNavigation:withError:)에 대응합니다.
                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    Log.e(TAG, "Page Error: ${error?.description} at ${request?.url}")
                    // TODO: 에러 UI 표시 (필요 시)
                }
            }
        }
    }

    // ── 5. 시스템 뒤로가기 처리 ──────────────────────────────────
    // [개념] BackHandler는 시스템 뒤로가기(제스처/버튼)를 가로채는 Composable API입니다.
    //        NavHost 기본 BackHandler보다 더 가까운(inner) 스코프에 등록되므로 우선 실행됩니다.
    //
    // 처리 전략:
    //   - WebView 내부에 방문 히스토리가 있으면 → webView.goBack()으로 웹 내 뒤로가기
    //   - 히스토리가 없으면(첫 페이지) → onNavigateBack() 호출로 화면 닫기
    //
    // 이 처리 없이 NavHost에 위임하면, 사용자가 뒤로가기를 빠르게 2번 누를 때
    // popBackStack()이 중복 호출되어 webview 화면뿐 아니라 부모(설정 화면)까지 닫히는
    // 버그가 발생합니다. iOS의 navigationBarBackButtonHidden + 커스텀 dismiss에 대응합니다.
    BackHandler(enabled = true) {
        if (webView.canGoBack()) {
            // 웹 내부 히스토리가 남아 있으면 WebView 안에서 뒤로 이동합니다.
            webView.goBack()
        } else {
            // 더 이상 돌아갈 웹 히스토리가 없으면 Compose 화면을 닫습니다.
            onNavigateBack()
        }
    }

    // ── 6. AndroidView로 Compose에 통합 ──────────────────────────
    // [개념] statusBarsPadding()은 상태바 높이만큼 top padding을 자동으로 추가합니다.
    //        iOS의 NavigationStack safe area 처리와 동일한 효과입니다.
    //        navigationBarsPadding()은 하단 제스처바 영역과의 겹침을 방지합니다.
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        factory = { webView },
        update = { view ->
            val extraHeaders = buildAppInfoHeaders()
            Log.d(TAG, "Loading URL with headers: $url")
            view.loadUrl(url, extraHeaders)
        }
    )
}

// ── JS Bridge 인터페이스 ──────────────────────────────────────────

/**
 * JavaScript에서 호출 가능한 Native 브릿지 인터페이스.
 *
 * [개념] @JavascriptInterface 어노테이션이 붙은 public 메서드만 JavaScript에서 접근 가능합니다.
 *        iOS의 WKScriptMessageHandler userContentController(_:didReceive:)에 대응합니다.
 */
private class AppBridgeInterface(private val onClose: () -> Unit) {

    /**
     * 웹 페이지에서 window.Android.postMessage("close") 호출 시 실행됩니다.
     *
     * iOS: window.webkit.messageHandlers.appBridge.postMessage("close")
     */
    @JavascriptInterface
    fun postMessage(message: String) {
        Log.d("SeaThermoWebView", "JS Message Received: $message")
        when (message) {
            "close" -> onClose()
            // 향후 share, login 등 메시지 추가 가능
        }
    }
}

// ── 앱 정보 헤더 ──────────────────────────────────────────────────

/**
 * 온바다 서버 요청에 포함할 앱/디바이스 정보 헤더를 생성합니다.
 *
 * iOS의 APIEndpoints.Headers.appInfo()에 대응합니다.
 * 서버에서 앱 버전, OS 종류, 디바이스 모델 등을 식별하는 데 사용됩니다.
 */
private fun buildAppInfoHeaders(): Map<String, String> {
    return mapOf(
        // iOS: "X-App-Version": appVersion
        "X-App-Version" to BuildConfig.VERSION_NAME,
        // iOS: "X-App-Build": appBuild
        "X-App-Build" to BuildConfig.VERSION_CODE.toString(),
        // iOS: "X-OS-Name": "iOS" → Android에서는 "Android"
        "X-OS-Name" to "Android",
        // iOS: "X-OS-Version": UIDevice.current.systemVersion
        "X-OS-Version" to Build.VERSION.RELEASE,
        // iOS: "X-Device-Model": sysctl 하드웨어 식별자
        // [개념] Build.MODEL은 Android 디바이스 모델명을 반환합니다. (예: "Pixel 8 Pro")
        "X-Device-Model" to Build.MODEL,
        // iOS: "X-Bundle-Id": Bundle.main.bundleIdentifier
        "X-Bundle-Id" to "com.onbada.seathermo"
    )
}

// ── JavaScript 심(Shim) ───────────────────────────────────────────

/**
 * iOS WKWebView의 webkit 메시지 브릿지를 Android에서 동일하게 동작시키는 JavaScript 코드.
 *
 * 서버 웹 페이지가 iOS 방식으로 브릿지를 호출하도록 구현되어 있을 때,
 * 이 심을 주입하면 Android에서도 동일한 코드가 동작합니다.
 *
 * 웹 페이지 JS 호출 방식:
 *   window.webkit.messageHandlers.appBridge.postMessage("close")
 * ↓ 심을 통해 Android 네이티브 브릿지로 전달
 *   window.Android.postMessage("close")
 */
private const val WEBKIT_BRIDGE_SHIM = """
    (function() {
        if (typeof window.webkit === 'undefined') {
            window.webkit = {
                messageHandlers: {
                    appBridge: {
                        postMessage: function(msg) {
                            if (window.Android) {
                                window.Android.postMessage(msg);
                            }
                        }
                    }
                }
            };
        }
    })();
"""
