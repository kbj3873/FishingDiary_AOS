package com.bj.fishingdiary.infrastructure.network

import android.net.Uri
import com.google.gson.Gson
import java.net.URLEncoder

/**
 * HTTP 메서드 타입
 * HTTP Method Type
 *
 * iOS의 HTTPMethodType에 대응
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
 * Body 인코더 인터페이스
 * Body Encoder Interface
 */
interface BodyEncoder {
    fun encode(parameters: Map<String, Any>): ByteArray?
}

/**
 * JSON Body 인코더
 * JSON Body Encoder
 */
class JSONBodyEncoder : BodyEncoder {
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
 * ASCII Body 인코더 (Query String 형태)
 * ASCII Body Encoder (Query String format)
 */
class AsciiBodyEncoder : BodyEncoder {
    override fun encode(parameters: Map<String, Any>): ByteArray? {
        return parameters.toQueryString().toByteArray(Charsets.US_ASCII)
    }
}

/**
 * Response Decoder 인터페이스
 * Response Decoder Interface
 */
interface ResponseDecoder {
    fun <T> decode(data: ByteArray, type: Class<T>): T
}

/**
 * JSON Response Decoder
 */
class JSONResponseDecoder : ResponseDecoder {
    private val gson = Gson()

    override fun <T> decode(data: ByteArray, type: Class<T>): T {
        val json = String(data, Charsets.UTF_8)
        return gson.fromJson(json, type)
    }
}

/**
 * Requestable 인터페이스
 * Requestable Interface
 *
 * iOS의 Requestable protocol에 대응
 */
interface Requestable {
    val path: String
    val isFullPath: Boolean
    val method: HTTPMethodType
    val headerParameters: Map<String, String>
    val queryParametersEncodable: Any?
    val queryParameters: Map<String, Any>
    val bodyParametersEncodable: Any?
    val bodyParameters: Map<String, Any>
    val bodyEncoder: BodyEncoder

    /**
     * NetworkConfig를 사용하여 URL 생성
     * Generate URL using NetworkConfig
     */
    fun url(config: NetworkConfigurable): String {
        val baseURL = if (config.baseURL.last() != '/') {
            "${config.baseURL}/"
        } else {
            config.baseURL
        }

        val endpoint = if (isFullPath) path else "$baseURL$path"

        // Query Parameters 조합
        val allQueryParams = mutableMapOf<String, String>()

        // queryParametersEncodable을 Dictionary로 변환 (TODO: 실제 구현 시 Gson 활용)
        queryParameters.forEach { (key, value) ->
            allQueryParams[key] = value.toString()
        }

        // Config의 queryParameters 추가
        config.queryParameters.forEach { (key, value) ->
            allQueryParams[key] = value
        }

        return if (allQueryParams.isNotEmpty()) {
            "$endpoint?${allQueryParams.toQueryString()}"
        } else {
            endpoint
        }
    }
}

/**
 * ResponseRequestable 인터페이스
 * Response Requestable Interface
 *
 * iOS의 ResponseRequestable protocol에 대응
 */
interface ResponseRequestable<R> : Requestable {
    val responseDecoder: ResponseDecoder
}

/**
 * Endpoint 클래스
 * Endpoint Class
 *
 * iOS의 Endpoint<R> class에 대응
 */
data class Endpoint<R>(
    override val path: String,
    override val isFullPath: Boolean = false,
    override val method: HTTPMethodType,
    override val headerParameters: Map<String, String> = emptyMap(),
    override val queryParametersEncodable: Any? = null,
    override val queryParameters: Map<String, Any> = emptyMap(),
    override val bodyParametersEncodable: Any? = null,
    override val bodyParameters: Map<String, Any> = emptyMap(),
    override val bodyEncoder: BodyEncoder = JSONBodyEncoder(),
    override val responseDecoder: ResponseDecoder = JSONResponseDecoder()
) : ResponseRequestable<R>

/**
 * Request Generation Error
 */
class RequestGenerationException(message: String) : Exception(message)

// ==================== Extension Functions ====================

/**
 * Map을 Query String으로 변환
 * Convert Map to Query String
 */
private fun Map<String, Any>.toQueryString(): String {
    return this.map { (key, value) ->
        val encodedKey = URLEncoder.encode(key, "UTF-8")
        val encodedValue = URLEncoder.encode(value.toString(), "UTF-8")
        "$encodedKey=$encodedValue"
    }.joinToString("&")
}
