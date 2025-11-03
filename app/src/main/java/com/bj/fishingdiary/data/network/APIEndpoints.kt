package com.bj.fishingdiary.data.network

/**
 * API 엔드포인트 정의
 * API Endpoints Definition
 *
 * iOS의 APIEndpoints.swift에 대응
 * 앱에서 사용하는 모든 API 엔드포인트를 정의
 */
object APIEndpoints {

    /**
     * RISA JSON API 엔드포인트
     * RISA JSON API Endpoint
     *
     * @param requestDTO 요청 DTO
     * @return Endpoint 객체
     */
    fun <T, R> getRisaJson(requestDTO: T): Endpoint<R> {
        return Endpoint(
            path = "OpenAPI_json",
            method = HttpMethod.POST,
            // TODO: queryParameters 구현
        )
    }

    /**
     * RISA XML API 엔드포인트
     * RISA XML API Endpoint
     *
     * @param requestDTO 요청 DTO
     * @return Endpoint 객체
     */
    fun <T, R> getRisaXml(requestDTO: T): Endpoint<R> {
        return Endpoint(
            path = "risa/risaInfo.risa",
            method = HttpMethod.POST,
            // TODO: queryParameters 구현
        )
    }
}

/**
 * HTTP 메서드 열거형
 * HTTP Method Enumeration
 */
enum class HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH
}

/**
 * API 엔드포인트 모델
 * API Endpoint Model
 *
 * @param path API 경로
 * @param method HTTP 메서드
 * @param headers HTTP 헤더
 * @param queryParameters 쿼리 파라미터
 * @param bodyParameters Body 파라미터
 */
data class Endpoint<R>(
    val path: String,
    val method: HttpMethod,
    val headers: Map<String, String> = emptyMap(),
    val queryParameters: Map<String, String> = emptyMap(),
    val bodyParameters: Map<String, Any> = emptyMap()
)
