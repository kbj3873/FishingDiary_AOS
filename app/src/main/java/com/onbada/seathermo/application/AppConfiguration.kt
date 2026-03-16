package com.onbada.seathermo.application

import com.onbada.seathermo.BuildConfig

/**
 * 앱 전역 설정 클래스
 * App Configuration Class
 *
 * iOS의 AppConfiguration.swift에 대응
 * BuildConfig에서 API 키와 Base URL을 읽어옴
 */
class AppConfiguration {

    /**
     * RISA API 키 (실시간 해양수산환경 관측시스템)
     * RISA API Key (Real-time Marine Environment Observation System)
     */
    val apiKeyRisa: String
        get() {
            require(BuildConfig.RISA_API_KEY.isNotEmpty()) {
                "RISA_API_KEY must not be empty in local.properties"
            }
            return BuildConfig.RISA_API_KEY
        }

    /**
     * NIFS API Base URL (국립수산과학원 해양 데이터).
     *
     * iOS의 apiNifsURL에 대응합니다.
     */
    val apiNifsURL: String
        get() {
            require(BuildConfig.API_BASE_URL.isNotEmpty()) {
                "API_BASE_URL must not be empty in local.properties"
            }
            return BuildConfig.API_BASE_URL
        }

    /**
     * 온바다 서버 API Base URL (자체 서비스).
     *
     * iOS의 apiOnbadaURL에 대응합니다.
     */
    val apiOnbadaURL: String
        get() {
            require(BuildConfig.ONBADA_BASE_URL.isNotEmpty()) {
                "ONBADA_BASE_URL must not be empty in local.properties"
            }
            return BuildConfig.ONBADA_BASE_URL
        }
}
