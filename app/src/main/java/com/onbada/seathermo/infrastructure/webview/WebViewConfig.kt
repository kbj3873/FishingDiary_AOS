package com.onbada.seathermo.infrastructure.webview

/**
 * 온바다 서버 웹뷰 페이지 정의.
 *
 * [개념] enum class의 생성자 파라미터(val path: String)로 각 상수에 값을 연결합니다.
 *        Swift의 enum WebPage: String { case notices = "/notices-page/" }에 대응합니다.
 *        WebPage.NOTICES.path 처럼 접근합니다.
 *
 * iOS의 Infrastructure/WebView/WebViewConfig.swift WebPage enum에 대응합니다.
 */
enum class WebPage(val path: String) {
    NOTICES("/notices-page/"),   // 공지사항
    LICENSES("/licenses/")      // 오픈소스 라이선스
}
