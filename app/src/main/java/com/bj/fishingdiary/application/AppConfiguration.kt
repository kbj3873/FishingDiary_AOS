package com.bj.fishingdiary.application

import com.bj.fishingdiary.BuildConfig

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
     * API Base URL
     */
    val apiBaseURL: String
        get() {
            require(BuildConfig.API_BASE_URL.isNotEmpty()) {
                "API_BASE_URL must not be empty in local.properties"
            }
            return BuildConfig.API_BASE_URL
        }
}
