package com.onbada.seathermo.infrastructure.network

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder

/**
 * HTTP 메서드 타입.
 *
 * [개념] enum class는 정해진 상수 집합을 타입으로 정의합니다.
 *        각 상수에 값(value)을 연결하면 .value로 원시 문자열에 접근할 수 있습니다.
 *        Swift의 enum HTTPMethodType: String { case get = "GET" }에 대응합니다.
 *
 * iOS의 HTTPMethodType에 대응합니다.
 */
enum class HTTPMethodType(val value: String) {
    GET("GET"),
    HEAD("HEAD"),
    POST("POST"),
    PUT("PUT"),
    PATCH("PATCH"),
    DELETE("DELETE")
}

/**
 * Body 인코더 인터페이스.
 *
 * [개념] interface는 Swift의 protocol과 동일합니다.
 *        Body 파라미터를 ByteArray로 변환하는 전략(strategy) 패턴입니다.
 *
 * iOS의 BodyEncoder protocol에 대응합니다.
 */
interface BodyEncoder {
    fun encode(parameters: Map<String, Any>): ByteArray?
}

/**
 * JSON Body 인코더.
 *
 * 파라미터를 JSON 문자열로 변환 후 UTF-8 바이트 배열로 인코딩합니다.
 * iOS의 JSONBodyEncoder에 대응합니다.
 */
class JSONBodyEncoder : BodyEncoder {
    // [개념] Gson은 Google이 만든 JSON 직렬화/역직렬화 라이브러리입니다.
    //        Swift의 JSONEncoder/JSONDecoder에 대응합니다.
    private val gson = Gson()

    override fun encode(parameters: Map<String, Any>): ByteArray? {
        return try {
            gson.toJson(parameters).toByteArray(Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * ASCII Body 인코더 (form-encoded).
 *
 * 파라미터를 application/x-www-form-urlencoded 형식으로 인코딩합니다.
 * iOS의 AsciiBodyEncoder에 대응합니다.
 */
class AsciiBodyEncoder : BodyEncoder {
    override fun encode(parameters: Map<String, Any>): ByteArray? {
        return parameters.toQueryString().toByteArray(Charsets.US_ASCII)
    }
}

// MARK: - Endpoint

/**
 * 제네릭 네트워크 엔드포인트.
 *
 * [개념] data class는 equals/hashCode/toString/copy를 자동 생성하는 클래스입니다.
 *        Swift의 struct와 유사하게 값 비교가 가능합니다.
 *
 * [개념] 타입 파라미터 <R>은 이 엔드포인트의 응답 DTO 타입을 나타냅니다.
 *        예: Endpoint<RisaListResponseDTO>이면 응답을 RisaListResponseDTO로 디코딩합니다.
 *        iOS의 Endpoint<Response>에 대응합니다.
 *
 * iOS의 Endpoint.swift Endpoint<Response> class에 대응합니다.
 */
data class Endpoint<R>(
    val baseURL: String,
    val path: String,
    val isFullPath: Boolean = false,
    val method: HTTPMethodType,
    val headerParameters: Map<String, String> = emptyMap(),
    val queryParameters: Map<String, Any> = emptyMap(),
    val bodyParameters: Map<String, Any> = emptyMap(),
    val bodyEncoder: BodyEncoder = JSONBodyEncoder()
) {

    /**
     * 엔드포인트의 전체 URL 문자열을 생성합니다.
     *
     * [개념] if 표현식은 값을 반환할 수 있습니다.
     *        Swift의 let basePath = baseURL.last == "/" ? baseURL : baseURL + "/"와 동일합니다.
     *
     * iOS의 Endpoint.url()에 대응합니다.
     */
    fun url(): String {
        // [개념] endsWith()는 문자열이 특정 접미사로 끝나는지 확인합니다.
        //        Swift의 hasSuffix("/")에 대응합니다.
        val basePath = if (baseURL.endsWith("/")) baseURL else "$baseURL/"
        val endpointString = if (isFullPath) path else "$basePath$path"

        // 쿼리 파라미터가 없으면 경로만 반환합니다.
        if (queryParameters.isEmpty()) return endpointString

        // [개념] joinToString()은 컬렉션 요소를 구분자로 연결해 하나의 문자열로 합칩니다.
        //        Swift의 .joined(separator: "&")에 대응합니다.
        val queryString = queryParameters.map { (key, value) ->
            val encodedKey = URLEncoder.encode(key, "UTF-8")
            val encodedValue = URLEncoder.encode(value.toString(), "UTF-8")
            "$encodedKey=$encodedValue"
        }.joinToString("&")

        return "$endpointString?$queryString"
    }

    /**
     * OkHttp Request 객체를 생성합니다.
     *
     * iOS의 Endpoint.urlRequest() → URLRequest에 대응합니다.
     * Android에서는 OkHttp의 Request 객체를 반환합니다.
     *
     * [개념] OkHttp의 Request.Builder()는 빌더 패턴으로 Request를 구성합니다.
     *        .url().addHeader().method().build() 체이닝으로 객체를 완성합니다.
     *        iOS의 var urlRequest = URLRequest(url: url)에 대응합니다.
     */
    fun urlRequest(): Request {
        val requestBuilder = Request.Builder().url(url())

        // 헤더 설정
        headerParameters.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }

        // Body 설정
        // [개념] iOS의 urlRequest()는 httpBody를 직접 설정하고 Content-Type은 헤더에서만 관리합니다.
        //        OkHttp의 toRequestBody(mediaType)은 mediaType을 Content-Type 헤더로 자동 추가하므로
        //        headerParameters에 설정된 Content-Type과 충돌하거나 덮어쓸 수 있습니다.
        //        따라서 mediaType을 하드코딩하지 않고 headerParameters의 Content-Type을 사용합니다.
        if (bodyParameters.isNotEmpty()) {
            val bodyData = bodyEncoder.encode(bodyParameters)
            if (bodyData != null) {
                val contentType = (headerParameters["Content-Type"] ?: "application/json; charset=utf-8")
                    .toMediaType()
                requestBuilder.method(method.value, bodyData.toRequestBody(contentType))
            }
        } else {
            // [개념] GET/HEAD/DELETE는 body 없이, POST/PUT/PATCH는 빈 body로 전송합니다.
            //        HTTP 스펙에서 GET 요청에 body를 포함하면 일부 서버가 거부할 수 있습니다.
            when (method) {
                HTTPMethodType.GET, HTTPMethodType.HEAD, HTTPMethodType.DELETE ->
                    requestBuilder.method(method.value, null)
                else ->
                    requestBuilder.method(method.value, ByteArray(0).toRequestBody())
            }
        }

        return requestBuilder.build()
    }
}

// 요청 생성 에러. iOS의 RequestGenerationError에 대응합니다.
class RequestGenerationException(message: String) : Exception(message)

// ==================== Private Extensions ====================

/**
 * Map을 URL Query String으로 변환합니다.
 *
 * [개념] private fun Map<...>.toQueryString()는 확장 함수(extension function)입니다.
 *        기존 클래스에 새로운 메서드를 추가하는 Kotlin 고유 기능입니다.
 *        Swift의 private extension Dictionary { var queryString: String }에 대응합니다.
 */
private fun Map<String, Any>.toQueryString(): String {
    return this.map { (key, value) ->
        val encodedKey = URLEncoder.encode(key, "UTF-8")
        val encodedValue = URLEncoder.encode(value.toString(), "UTF-8")
        "$encodedKey=$encodedValue"
    }.joinToString("&")
}
