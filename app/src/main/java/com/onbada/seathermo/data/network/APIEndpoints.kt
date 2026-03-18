package com.onbada.seathermo.data.network

import android.os.Build
import com.onbada.seathermo.BuildConfig
import com.onbada.seathermo.data.network.datamapping.RegionResponseDTO
import com.onbada.seathermo.data.network.datamapping.RisaInfoListRequestDTO
import com.onbada.seathermo.data.network.datamapping.RisaInfoResponseDTO
import com.onbada.seathermo.data.network.datamapping.RisaListRequestDTO
import com.onbada.seathermo.data.network.datamapping.RisaListResponseDTO
import com.onbada.seathermo.data.network.datamapping.VersionCheckRequestDTO
import com.onbada.seathermo.data.network.datamapping.VersionCheckResponseDTO
import com.onbada.seathermo.infrastructure.network.AsciiBodyEncoder
import com.onbada.seathermo.infrastructure.network.Endpoint
import com.onbada.seathermo.infrastructure.network.HTTPMethodType

/**
 * API 엔드포인트 팩토리.
 *
 * [개념] object 키워드는 싱글턴 객체를 선언합니다.
 *        클래스 인스턴스화 없이 APIEndpoints.getRisaJson(...)처럼 직접 호출합니다.
 *        Swift의 struct APIEndpoints { static func ... }와 동일한 역할입니다.
 *
 * iOS의 APIEndpoints.swift에 대응합니다.
 */
object APIEndpoints {

    // ==================== NIFS Open API ====================

    /**
     * 실시간 수온 목록 조회 엔드포인트 (NIFS JSON API).
     *
     * iOS의 Endpoint(baseURL: baseURL, path: "OpenAPI_json", ...)에 대응합니다.
     *
     * @param baseURL RISA API 베이스 URL.
     * @param requestDTO 조회 조건 DTO.
     */
    fun getRisaJson(
        baseURL: String,
        requestDTO: RisaListRequestDTO
    ): Endpoint<RisaListResponseDTO> = Endpoint(
        baseURL = baseURL,
        path = "OpenAPI_json",
        method = HTTPMethodType.POST,
        headerParameters = Headers.forOpenAPI(isJson = true),
        queryParameters = requestDTO.toQueryParams()
    )

    /**
     * 수온 이력 목록 조회 엔드포인트 (NIFS 웹 크롤링 우회 API).
     *
     * form-encoded body 방식으로 전송합니다.
     * WAF(보안장비) 우회를 위해 모바일 브라우저 User-Agent를 사용합니다.
     *
     * @param baseURL NIFS 웹 API 베이스 URL.
     * @param requestDTO 조회 조건 DTO.
     */
    fun searchRisaInfoList(
        baseURL: String,
        requestDTO: RisaInfoListRequestDTO
    ): Endpoint<RisaInfoResponseDTO> = Endpoint(
        baseURL = baseURL,
        path = "risa/risa/risaA/searchRisaInfoList.do",
        method = HTTPMethodType.POST,
        headerParameters = Headers.forWebCrawling(),
        bodyParameters = requestDTO.toBodyParams(),
        // [개념] AsciiBodyEncoder()는 파라미터를 application/x-www-form-urlencoded 형식으로 인코딩합니다.
        //        iOS의 bodyEncoder: AsciiBodyEncoder()와 동일합니다.
        bodyEncoder = AsciiBodyEncoder()
    )

    // ==================== 온바다 서버 API ====================

    /**
     * 앱 버전 체크 엔드포인트.
     *
     * @param baseURL 온바다 서버 베이스 URL.
     * @param requestDTO 버전 정보 DTO.
     */
    fun postVersionCheck(
        baseURL: String,
        requestDTO: VersionCheckRequestDTO
    ): Endpoint<VersionCheckResponseDTO> = Endpoint(
        baseURL = baseURL,
        path = "api/version/check",
        method = HTTPMethodType.POST,
        headerParameters = Headers.forSeaThermoAPI(),
        bodyParameters = requestDTO.toBodyParams()
    )

    /**
     * 관측소 목록 조회 엔드포인트.
     *
     * @param baseURL 온바다 서버 베이스 URL.
     */
    fun postRegions(baseURL: String): Endpoint<RegionResponseDTO> = Endpoint(
        baseURL = baseURL,
        path = "api/regions",
        method = HTTPMethodType.POST,
        headerParameters = Headers.forSeaThermoAPI()
    )

    // ==================== 헤더 팩토리 ====================

    /**
     * 공통 헤더 팩토리.
     *
     * [개념] object 안의 object는 중첩 싱글턴입니다.
     *        iOS의 extension APIEndpoints { struct Headers { ... } }에 대응합니다.
     */
    object Headers {

        /**
         * NIFS Open API 공통 헤더.
         *
         * @param isJson true이면 JSON, false이면 form-encoded Content-Type을 사용합니다.
         */
        fun forOpenAPI(isJson: Boolean = true): Map<String, String> = mapOf(
            "Accept-Language" to "ko-KR,ko;q=0.9",
            "Content-Type" to if (isJson) "application/json;utf-8" else "application/x-www-form-urlencoded"
        )

        /**
         * NIFS 웹 크롤링 우회 헤더.
         *
         * WAF(Web Application Firewall) 우회를 위해 모바일 브라우저 User-Agent를 사용합니다.
         * 실제 기기 정보를 동적으로 주입합니다.
         */
        fun forWebCrawling(): Map<String, String> = mapOf(
            "Referer" to "https://www.nifs.go.kr/risa/risa/risaA/actionRisaInfo.do",
            "X-Requested-With" to "XMLHttpRequest",
            "Content-Type" to "application/x-www-form-urlencoded",
            "User-Agent" to webCrawlingUserAgent
        )

        // [개념] get() = ... 은 매번 호출 시 실제 기기 정보를 읽어 계산합니다.
        private val webCrawlingUserAgent: String
            get() = "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL}) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Mobile Safari/537.36"

        /**
         * 온바다 서버 API 공통 헤더.
         *
         * 앱/기기 정보를 커스텀 헤더로 전송합니다.
         */
        fun forSeaThermoAPI(): Map<String, String> {
            // [개념] +로 두 Map을 병합합니다. 중복 키는 오른쪽(appInfo) 값이 우선합니다.
            val base = mapOf(
                "Content-Type" to "application/json",
                "Accept-Language" to "ko-KR,ko;q=0.9",
                "User-Agent" to defaultUserAgent
            )
            return base + appInfo()
        }

        /**
         * 앱/기기 정보 헤더 딕셔너리.
         *
         * [개념] BuildConfig는 build.gradle에서 주입된 빌드 시점 상수를 담습니다.
         *        iOS의 Bundle.main.infoDictionary에 대응합니다.
         *
         *        Build.VERSION.RELEASE는 Android OS 버전 문자열입니다 (예: "14").
         *        iOS의 UIDevice.current.systemVersion에 대응합니다.
         *
         *        Build.MODEL은 기기 모델명입니다 (예: "Pixel 8").
         *        iOS의 sysctl로 읽는 하드웨어 식별자에 대응합니다.
         */
        fun appInfo(): Map<String, String> = mapOf(
            "X-App-Version" to BuildConfig.VERSION_NAME,
            "X-App-Build" to BuildConfig.VERSION_CODE.toString(),
            "X-OS-Name" to "Android",
            "X-OS-Version" to Build.VERSION.RELEASE,
            "X-Device-Model" to Build.MODEL,
            "X-Bundle-Id" to BuildConfig.APPLICATION_ID
        )

        // [개념] get() = ... 은 매번 호출 시 계산되는 프로퍼티입니다.
        //        Swift의 static var defaultUserAgent: String { ... }에 대응합니다.
        private val defaultUserAgent: String
            get() = "SeaThermo/${BuildConfig.VERSION_NAME} (Android ${Build.VERSION.RELEASE}; ${Build.MODEL})"
    }
}
