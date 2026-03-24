package com.onbada.seathermo.infrastructure.webview

/**
 * 온바다 서버 웹뷰 페이지 정의.
 *
 * [개념] enum class의 생성자 파라미터로 각 상수에 값을 연결합니다.
 *        Swift의 enum WebPage: String { case notices = "/notices-page/" }에 대응합니다.
 *        WebPage.NOTICES.path 처럼 접근합니다.
 *
 * iOS의 Infrastructure/WebView/WebViewConfig.swift WebPage enum에 대응합니다.
 *
 * @property path 온바다 서버 Base URL에 붙는 경로
 * @property key  Navigation Route 식별자 (URL 경로에 포함 가능한 단순 문자열)
 */
enum class WebPage(val path: String, val key: String) {
    NOTICES("/notices-page/", "notices"),   // 공지사항
    LICENSES("/licenses/", "licenses");     // 오픈소스 라이선스

    companion object {
        /**
         * Navigation Route의 key 문자열로 WebPage를 역조회합니다.
         *
         * [개념] companion object는 Java의 static 메서드에 해당합니다.
         *        Swift의 static func fromKey(_:) -> WebPage?에 대응합니다.
         */
        fun fromKey(key: String): WebPage? = entries.find { it.key == key }
    }
}
