package com.bj.fishingdiary.infrastructure.network

import android.util.Log
import com.bj.fishingdiary.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.nio.charset.Charset

/**
 * Network Error
 *
 * iOS의 NetworkError에 대응
 */
sealed class NetworkError : Exception() {
    data class HttpError(val statusCode: Int, val data: ByteArray?) : NetworkError() {
        override val message: String
            get() = "HTTP Error: $statusCode"
    }

    object NotConnected : NetworkError() {
        override val message: String
            get() = "Not connected to internet"
    }

    object Cancelled : NetworkError() {
        override val message: String
            get() = "Request cancelled"
    }

    data class Generic(val error: Throwable) : NetworkError() {
        override val message: String
            get() = "Generic error: ${error.message}"
    }

    object UrlGeneration : NetworkError() {
        override val message: String
            get() = "URL generation failed"
    }

    object NoData : NetworkError() {
        override val message: String
            get() = "No response data"
    }
}

/**
 * Network Service 인터페이스
 * Network Service Interface
 *
 * iOS의 NetworkService protocol에 대응
 */
interface NetworkService {
    fun request(
        endpoint: Requestable,
        completion: (Result<ByteArray?>) -> Unit
    ): NetworkCancellable?
}

/**
 * Network Session Manager 인터페이스
 * Network Session Manager Interface
 */
interface NetworkSessionManager {
    fun request(
        request: Request,
        completion: (ByteArray?, Response?, Throwable?) -> Unit
    ): NetworkCancellable
}

/**
 * Network Error Logger 인터페이스
 * Network Error Logger Interface
 */
interface NetworkErrorLogger {
    fun log(request: Request)
    fun log(responseData: ByteArray?, response: Response?)
    fun log(error: Throwable)
}

/**
 * Default Network Service 구현체
 * Default Network Service Implementation
 *
 * iOS의 DefaultNetworkService에 대응
 */
class DefaultNetworkService(
    private val config: NetworkConfigurable,
    private val sessionManager: NetworkSessionManager = DefaultNetworkSessionManager(),
    private val logger: NetworkErrorLogger = DefaultNetworkErrorLogger()
) : NetworkService {

    // EUC-KR 인코딩 (www.nifs.go.kr에서 사용)
    private val eucKrCharset = Charset.forName("EUC-KR")

    override fun request(
        endpoint: Requestable,
        completion: (Result<ByteArray?>) -> Unit
    ): NetworkCancellable? {
        return try {
            val urlRequest = buildRequest(endpoint, config)
            request(urlRequest, completion)
        } catch (e: Exception) {
            completion(Result.failure(NetworkError.UrlGeneration))
            null
        }
    }

    private fun request(
        request: Request,
        completion: (Result<ByteArray?>) -> Unit
    ): NetworkCancellable {
        logger.log(request)

        return sessionManager.request(request) { data, response, error ->
            if (error != null) {
                val networkError = when {
                    response != null -> NetworkError.HttpError(
                        response.code,
                        data
                    )
                    else -> resolveError(error)
                }

                logger.log(networkError)
                completion(Result.failure(networkError))
            } else {
                val resolvedData = resolveData(data)
                if (resolvedData == null) {
                    completion(Result.failure(NetworkError.NoData))
                } else {
                    logger.log(resolvedData, response)
                    completion(Result.success(resolvedData))
                }
            }
        }
    }

    private fun resolveError(error: Throwable): NetworkError {
        return when {
            error is IOException && error.message?.contains("canceled") == true ->
                NetworkError.Cancelled
            error is IOException ->
                NetworkError.NotConnected
            else ->
                NetworkError.Generic(error)
        }
    }

    /**
     * EUC-KR → UTF-8 변환
     * www.nifs.go.kr에서 EUC-KR로 인코딩된 데이터를 UTF-8로 변환
     */
    private fun resolveData(data: ByteArray?): ByteArray? {
        if (data == null) return null

        return try {
            // EUC-KR로 디코딩 시도
            val eucKrString = String(data, eucKrCharset)
            eucKrString.toByteArray(Charsets.UTF_8)
        } catch (e: Exception) {
            // EUC-KR 디코딩 실패 시 원본 반환
            data
        }
    }

    private fun buildRequest(endpoint: Requestable, config: NetworkConfigurable): Request {
        val url = endpoint.url(config)

        val requestBuilder = Request.Builder().url(url)

        // Headers 설정
        val allHeaders = config.headers.toMutableMap()
        allHeaders.putAll(endpoint.headerParameters)
        allHeaders.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }

        // Body Parameters 설정
        if (endpoint.bodyParameters.isNotEmpty()) {
            val bodyData = endpoint.bodyEncoder.encode(endpoint.bodyParameters)
            if (bodyData != null) {
                val mediaType = "application/json; charset=utf-8".toMediaType()
                requestBuilder.method(
                    endpoint.method.value,
                    bodyData.toRequestBody(mediaType)
                )
            }
        } else {
            // Body가 없는 경우
            when (endpoint.method) {
                HTTPMethodType.GET, HTTPMethodType.HEAD, HTTPMethodType.DELETE ->
                    requestBuilder.method(endpoint.method.value, null)
                else ->
                    requestBuilder.method(endpoint.method.value, ByteArray(0).toRequestBody())
            }
        }

        return requestBuilder.build()
    }
}

/**
 * Default Network Session Manager
 *
 * OkHttp를 사용한 기본 구현
 */
class DefaultNetworkSessionManager : NetworkSessionManager {
    private val client = OkHttpClient()

    override fun request(
        request: Request,
        completion: (ByteArray?, Response?, Throwable?) -> Unit
    ): NetworkCancellable {
        val call = client.newCall(request)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                completion(null, null, e)
            }

            override fun onResponse(call: Call, response: Response) {
                val data = response.body?.bytes()
                completion(data, response, null)
            }
        })

        return object : NetworkCancellable {
            override fun cancel() {
                call.cancel()
            }
        }
    }
}

/**
 * Default Network Error Logger
 */
class DefaultNetworkErrorLogger : NetworkErrorLogger {
    companion object {
        private const val TAG = "NetworkService"
    }

    override fun log(request: Request) {
        Log.d(TAG, "-------------")
        Log.d(TAG, "request: ${request.url}")
        Log.d(TAG, "headers: ${request.headers}")
        Log.d(TAG, "method: ${request.method}")

        request.body?.let { body ->
            // Body 로깅 (개발 중에만)
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "body: [RequestBody]")
            }
        }
    }

    override fun log(responseData: ByteArray?, response: Response?) {
        if (responseData == null) return

        if (BuildConfig.DEBUG) {
            val dataString = String(responseData, Charsets.UTF_8)
            Log.d(TAG, "responseData: ${dataString.take(500)}") // 처음 500자만 로깅
        }
    }

    override fun log(error: Throwable) {
        Log.e(TAG, "Error: ${error.message}", error)
    }
}

/**
 * NetworkError extensions
 */
val NetworkError.isNotFoundError: Boolean
    get() = hasStatusCode(404)

fun NetworkError.hasStatusCode(code: Int): Boolean {
    return when (this) {
        is NetworkError.HttpError -> statusCode == code
        else -> false
    }
}
