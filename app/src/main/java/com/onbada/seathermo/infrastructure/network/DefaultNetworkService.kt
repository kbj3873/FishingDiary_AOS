package com.onbada.seathermo.infrastructure.network

import android.util.Log
import com.onbada.seathermo.BuildConfig
import okhttp3.*
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.charset.Charset
import javax.net.ssl.SSLException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

// ==================== Network Error ====================

/**
 * 네트워크 오류 sealed class.
 *
 * [개념] sealed class는 정해진 하위 타입만 존재하는 제한된 계층 구조입니다.
 *        Swift의 enum with associated values에 대응합니다.
 *        when 분기에서 모든 케이스를 처리하면 else가 불필요합니다.
 *
 * iOS의 NetworkError enum에 대응합니다.
 */
sealed class NetworkError : Exception() {
    data class HttpError(val statusCode: Int, val data: ByteArray?) : NetworkError() {
        override val message: String get() = "HTTP Error: $statusCode"
    }

    object NotConnected : NetworkError() {
        override val message: String get() = "Not connected to internet"
    }

    object Cancelled : NetworkError() {
        override val message: String get() = "Request cancelled"
    }

    data class Generic(val error: Throwable) : NetworkError() {
        override val message: String get() = "Generic error: ${error.message}"
    }

    object UrlGeneration : NetworkError() {
        override val message: String get() = "URL generation failed"
    }

    object NoData : NetworkError() {
        override val message: String get() = "No response data"
    }

    // iOS의 .apiError(code:message:)에 대응합니다.
    data class ApiError(val code: String, val apiMessage: String = "") : NetworkError() {
        override val message: String get() = "API Error: $code $apiMessage"
    }
}

// ==================== Network Service ====================

/**
 * 네트워크 서비스 인터페이스.
 *
 * [개념] interface는 Swift의 protocol과 동일합니다.
 *        구현체(DefaultNetworkService)와 분리하여 테스트 용이성을 확보합니다.
 *
 * iOS의 NetworkService protocol에 대응합니다.
 */
interface NetworkService {
    // 엔드포인트 팩토리에서 사용할 베이스 URL.
    // iOS의 var baseURL: String { get }에 대응합니다.
    val baseURL: String

    /**
     * 엔드포인트에 네트워크 요청을 수행하고 원시 바이트 데이터를 반환합니다.
     *
     * [개념] suspend fun은 코루틴 안에서만 호출할 수 있는 일시 중단 함수입니다.
     *        Swift의 async func에 대응합니다.
     *        호출 측에서 try-catch로 에러를 처리합니다.
     */
    suspend fun requestData(endpoint: Endpoint<*>): ByteArray
}

// ==================== Implementation ====================

/**
 * OkHttp 기반 NetworkService 구현체.
 *
 * [개념] class DefaultNetworkService(...) : NetworkService 는
 *        NetworkService 인터페이스를 구현하는 구체 클래스입니다.
 *        Swift의 final class DefaultNetworkService: NetworkService에 대응합니다.
 *
 * [개념] 생성자의 기본값 파라미터(= OkHttpClient() 등)를 통해
 *        테스트 시 가짜(mock) 객체를 주입할 수 있습니다.
 *        Swift의 init(session: URLSession = .shared)에 대응합니다.
 *
 * iOS의 DefaultNetworkService에 대응합니다.
 */
class DefaultNetworkService(
    override val baseURL: String,
    private val client: OkHttpClient = OkHttpClient(),
    private val logger: NetworkErrorLogger = DefaultNetworkErrorLogger()
) : NetworkService {

    // NIFS 웹사이트의 EUC-KR 인코딩 처리에 사용합니다.
    // iOS의 private let encEUC_KR에 대응합니다.
    private val eucKrCharset = Charset.forName("EUC-KR")

    /**
     * 네트워크 요청을 수행하고 응답 바이트 데이터를 반환합니다.
     *
     * [개념] suspendCancellableCoroutine { continuation -> }은
     *        콜백(callback) 기반 API를 코루틴(suspend) 방식으로 변환합니다.
     *        continuation.resume()으로 결과를 반환하고,
     *        continuation.resumeWithException()으로 에러를 전파합니다.
     *        Swift의 withCheckedThrowingContinuation { }에 대응합니다.
     *
     * [개념] OkHttp의 enqueue()는 비동기 HTTP 호출입니다.
     *        내부적으로 별도 스레드에서 네트워크 통신을 수행하고,
     *        완료 시 Callback의 onResponse/onFailure를 호출합니다.
     *        iOS의 URLSession.data(for: urlRequest)에 대응합니다.
     *
     * [개념] continuation.invokeOnCancellation { }은 코루틴이 취소될 때
     *        진행 중인 HTTP 요청도 함께 취소합니다.
     *        ViewModel이 소멸되면(예: 화면 전환) 불필요한 네트워크 요청이 자동 정리됩니다.
     */
    override suspend fun requestData(endpoint: Endpoint<*>): ByteArray {
        val okHttpRequest = endpoint.urlRequest()
        logger.log(okHttpRequest)

        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(okHttpRequest)

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // 원본 IOException을 먼저 로그로 남겨 실제 원인을 파악합니다.
                    logger.log(e)
                    val networkError = resolveError(e)
                    continuation.resumeWithException(networkError)
                }

                override fun onResponse(call: Call, response: Response) {
                    val data = response.body?.bytes()

                    // HTTP 상태 코드 검사 (200~299가 아니면 오류)
                    // [개념] response.isSuccessful은 HTTP 200~299 범위를 확인합니다.
                    //        iOS의 !(200...299).contains(httpResponse.statusCode)에 대응합니다.
                    if (!response.isSuccessful) {
                        val error = NetworkError.HttpError(response.code, data)
                        logger.log(error)
                        continuation.resumeWithException(error)
                        return
                    }

                    if (data == null) {
                        continuation.resumeWithException(NetworkError.NoData)
                        return
                    }

                    // EUC-KR → UTF-8 변환 (NIFS 웹사이트 대응 — Content-Type에 euc-kr이 있을 때만 적용)
                    val resolvedData = resolveData(data, response)
                    logger.log(resolvedData, response)
                    continuation.resume(resolvedData)
                }
            })

            // 코루틴 취소 시 OkHttp Call도 취소합니다.
            continuation.invokeOnCancellation { call.cancel() }
        }
    }

    /**
     * 네트워크 에러를 NetworkError 타입으로 변환합니다.
     *
     * [개념] when { } 은 Swift의 switch 문과 동일하지만 조건식을 사용할 수 있습니다.
     *        is 키워드는 Swift의 case let error as IOException와 동일합니다.
     *
     * iOS의 private func resolve(error:)에 대응합니다.
     */
    private fun resolveError(error: Throwable): NetworkError {
        return when {
            error is IOException && error.message?.contains("canceled") == true ->
                NetworkError.Cancelled
            error is UnknownHostException ->
                // DNS 해석 실패 — 호스트를 찾을 수 없음 (잘못된 URL이거나 인터넷 미연결)
                NetworkError.NotConnected
            error is ConnectException ->
                // 서버에 연결 자체를 거부당함 (포트 오류, 서버 다운 등)
                NetworkError.NotConnected
            error is SocketTimeoutException ->
                // 응답 시간 초과
                NetworkError.Generic(error)
            error is SSLException ->
                // SSL/TLS 인증서 오류 (HTTP URL을 HTTPS로 요청하거나 인증서 불일치)
                NetworkError.Generic(error)
            error is IOException ->
                NetworkError.NotConnected
            else ->
                NetworkError.Generic(error)
        }
    }

    /**
     * 필요한 경우 EUC-KR 인코딩된 데이터를 UTF-8로 변환합니다.
     *
     * [개념] Content-Type 헤더의 charset을 확인하여 EUC-KR일 때만 변환합니다.
     *        JSON API(온바다 서버)는 이미 UTF-8이므로 변환하지 않습니다.
     *        NIFS 웹사이트는 EUC-KR로 응답하므로 변환이 필요합니다.
     */
    private fun resolveData(data: ByteArray, response: Response): ByteArray {
        val contentType = response.header("Content-Type") ?: ""
        val isEucKr = contentType.contains("euc-kr", ignoreCase = true)
        if (!isEucKr) return data

        return try {
            val eucKrString = String(data, eucKrCharset)
            eucKrString.toByteArray(Charsets.UTF_8)
        } catch (e: Exception) {
            // EUC-KR 디코딩 실패 시 원본 반환
            data
        }
    }
}

// ==================== Suspend Extension ====================

/**
 * NetworkService의 제네릭 suspend 확장 함수.
 *
 * 원시 바이트를 받아 Gson으로 지정된 타입 R로 디코딩합니다.
 *
 * [개념] inline fun + reified R: 제네릭 타입 R을 런타임에도 알 수 있게 합니다.
 *        일반적으로 Kotlin 제네릭은 런타임에 타입 정보가 사라지지만(Type Erasure),
 *        reified를 사용하면 R::class.java처럼 실제 타입에 접근할 수 있습니다.
 *        Gson이 JSON을 올바른 DTO 타입으로 역직렬화하기 위해 필요합니다.
 *
 * iOS에서는 Decodable 프로토콜 + 제네릭 func request<T: Decodable>이 이 역할을 수행합니다.
 * Kotlin에서는 Type Erasure 때문에 reified inline 확장 함수가 필요합니다.
 */
suspend inline fun <reified R : Any> NetworkService.request(
    endpoint: Endpoint<R>
): R {
    // [개념] reified 타입 파라미터를 람다 밖에서 미리 캡처합니다.
    //        R::class.java로 런타임 클래스 정보를 가져옵니다.
    val responseClass = R::class.java

    // [개념] as Endpoint<*>로 타입 파라미터를 지워서 기본 request(Endpoint<*>) 메서드를 호출합니다.
    //        스타 프로젝션(*)은 Swift의 Endpoint<Any>와 유사하게 타입 파라미터를 무시합니다.
    val data = requestData(endpoint as Endpoint<*>)
    val json = String(data, Charsets.UTF_8)
    return com.google.gson.Gson().fromJson(json, responseClass)
}

// ==================== Logger ====================

/**
 * 네트워크 에러 로거 인터페이스.
 *
 * iOS의 NetworkErrorLogger protocol에 대응합니다.
 */
interface NetworkErrorLogger {
    fun log(request: Request)
    fun log(responseData: ByteArray?, response: Response?)
    fun log(error: Throwable)
}

/**
 * 기본 네트워크 에러 로거 구현체.
 *
 * iOS의 DefaultNetworkErrorLogger에 대응합니다.
 *
 * [개념] companion object는 클래스에 속하는 정적(static) 멤버를 정의합니다.
 *        Swift의 static let TAG = "NetworkService"에 대응합니다.
 */
class DefaultNetworkErrorLogger : NetworkErrorLogger {
    companion object {
        private const val TAG = "NetworkService"
    }

    override fun log(request: Request) {
        if (!BuildConfig.DEBUG) return
        Log.d(TAG, "-------------")
        Log.d(TAG, "request: ${request.url}")
        Log.d(TAG, "method: ${request.method}")
        Log.d(TAG, "headers: ${request.headers}")

        // request body 로깅 (POST 등 body가 있는 경우)
        val body = request.body
        if (body != null) {
            try {
                val buffer = okio.Buffer()
                body.writeTo(buffer)
                Log.d(TAG, "body: ${buffer.readUtf8()}")
            } catch (e: Exception) {
                Log.d(TAG, "body: (read failed) ${e.message}")
            }
        } else {
            Log.d(TAG, "body: (none)")
        }
    }

    override fun log(responseData: ByteArray?, response: Response?) {
        if (!BuildConfig.DEBUG || responseData == null) return
        val raw = String(responseData, Charsets.UTF_8)

        // JSON Pretty Print: Gson으로 파싱 후 들여쓰기 포맷으로 재직렬화합니다.
        // [개념] runCatching은 try-catch를 함수형으로 표현합니다. 파싱 실패 시 원본 문자열을 사용합니다.
        val pretty = runCatching {
            val json = com.google.gson.Gson().fromJson(raw, Any::class.java)
            com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json)
        }.getOrDefault(raw)

        // Android Logcat은 한 번에 ~4000자까지만 출력합니다.
        // [개념] chunked(3000)은 문자열을 3000자 단위로 잘라 리스트로 반환합니다.
        //        각 청크를 별도 Log.d로 출력하여 전체 내용을 확인할 수 있습니다.
        Log.d(TAG, "responseData ↓")
        pretty.chunked(3000).forEachIndexed { index, chunk ->
            Log.d(TAG, "[$index] $chunk")
        }
    }

    override fun log(error: Throwable) {
        Log.e(TAG, "Error: ${error.message}", error)
    }
}

// ==================== NetworkError Extensions ====================

// [개념] 확장 프로퍼티로 404 체크 유틸리티를 추가합니다.
//        Swift의 extension NetworkError { var isNotFoundError: Bool }에 대응합니다.
val NetworkError.isNotFoundError: Boolean
    get() = hasStatusCode(404)

fun NetworkError.hasStatusCode(code: Int): Boolean {
    return when (this) {
        is NetworkError.HttpError -> statusCode == code
        else -> false
    }
}
