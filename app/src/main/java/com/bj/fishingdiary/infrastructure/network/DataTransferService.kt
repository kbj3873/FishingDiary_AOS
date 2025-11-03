package com.bj.fishingdiary.infrastructure.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

/**
 * Data Transfer Error
 *
 * iOS의 DataTransferError에 대응
 */
sealed class DataTransferError : Exception() {
    object NoResponse : DataTransferError() {
        override val message: String
            get() = "No response data"
    }

    data class Parsing(val error: Throwable) : DataTransferError() {
        override val message: String
            get() = "Parsing error: ${error.message}"
    }

    data class NetworkFailure(val networkError: NetworkError) : DataTransferError() {
        override val message: String
            get() = "Network failure: ${networkError.message}"
    }

    data class ResolvedNetworkFailure(val error: Throwable) : DataTransferError() {
        override val message: String
            get() = "Resolved network failure: ${error.message}"
    }
}

/**
 * Data Transfer Dispatch Queue 인터페이스
 * Data Transfer Dispatch Queue Interface
 *
 * iOS의 DataTransferDispatchQueue에 대응
 */
interface DataTransferDispatchQueue {
    fun asyncExecute(work: () -> Unit)
}

/**
 * Main Thread Dispatch Queue
 */
class MainThreadDispatchQueue : DataTransferDispatchQueue {
    private val handler = Handler(Looper.getMainLooper())

    override fun asyncExecute(work: () -> Unit) {
        handler.post(work)
    }
}

/**
 * Background Thread Dispatch Queue
 */
class BackgroundDispatchQueue : DataTransferDispatchQueue {
    override fun asyncExecute(work: () -> Unit) {
        Thread(work).start()
    }
}

/**
 * Data Transfer Service 인터페이스
 * Data Transfer Service Interface
 *
 * iOS의 DataTransferService protocol에 대응
 */
interface DataTransferService {
    /**
     * 일반 JSON 요청
     * Regular JSON request
     */
    fun <T, E : ResponseRequestable<T>> request(
        endpoint: E,
        responseType: Class<T>,
        queue: DataTransferDispatchQueue = MainThreadDispatchQueue(),
        completion: (Result<T>) -> Unit
    ): NetworkCancellable?

    /**
     * HTML 요청 (특별 파싱 필요)
     * HTML request (special parsing needed)
     */
    fun <T, E : ResponseRequestable<T>> requestHtml(
        endpoint: E,
        responseType: Class<T>,
        queue: DataTransferDispatchQueue = MainThreadDispatchQueue(),
        completion: (Result<T>) -> Unit
    ): NetworkCancellable?
}

/**
 * Data Transfer Error Resolver 인터페이스
 * Data Transfer Error Resolver Interface
 */
interface DataTransferErrorResolver {
    fun resolve(error: NetworkError): Throwable
}

/**
 * Data Transfer Error Logger 인터페이스
 * Data Transfer Error Logger Interface
 */
interface DataTransferErrorLogger {
    fun log(error: Throwable)
}

/**
 * Default Data Transfer Service 구현체
 * Default Data Transfer Service Implementation
 *
 * iOS의 DefaultDataTransferService에 대응
 */
class DefaultDataTransferService(
    private val networkService: NetworkService,
    private val errorResolver: DataTransferErrorResolver = DefaultDataTransferErrorResolver(),
    private val errorLogger: DataTransferErrorLogger = DefaultDataTransferErrorLogger()
) : DataTransferService {

    private val gson = Gson()

    override fun <T, E : ResponseRequestable<T>> request(
        endpoint: E,
        responseType: Class<T>,
        queue: DataTransferDispatchQueue,
        completion: (Result<T>) -> Unit
    ): NetworkCancellable? {
        return networkService.request(endpoint) { result ->
            result.onSuccess { data ->
                val decodeResult: Result<T> = decode(data, endpoint.responseDecoder, responseType)
                queue.asyncExecute { completion(decodeResult) }
            }.onFailure { error ->
                errorLogger.log(error)
                val resolvedError = resolveNetworkError(error as NetworkError)
                queue.asyncExecute { completion(Result.failure(resolvedError)) }
            }
        }
    }

    override fun <T, E : ResponseRequestable<T>> requestHtml(
        endpoint: E,
        responseType: Class<T>,
        queue: DataTransferDispatchQueue,
        completion: (Result<T>) -> Unit
    ): NetworkCancellable? {
        return networkService.request(endpoint) { result ->
            result.onSuccess { data ->
                val decodeResult: Result<T> = htmlDecode(data, responseType)
                queue.asyncExecute { completion(decodeResult) }
            }.onFailure { error ->
                errorLogger.log(error)
                val resolvedError = resolveNetworkError(error as NetworkError)
                queue.asyncExecute { completion(Result.failure(resolvedError)) }
            }
        }
    }

    // ==================== Private Functions ====================

    private fun <T> decode(
        data: ByteArray?,
        decoder: ResponseDecoder,
        type: Class<T>
    ): Result<T> {
        return try {
            if (data == null) {
                Result.failure(DataTransferError.NoResponse)
            } else {
                val result: T = decoder.decode(data, type)
                Result.success(result)
            }
        } catch (e: Exception) {
            errorLogger.log(e)
            Result.failure(DataTransferError.Parsing(e))
        }
    }

    private fun resolveNetworkError(error: NetworkError): DataTransferError {
        val resolvedError = errorResolver.resolve(error)
        return if (resolvedError is NetworkError) {
            DataTransferError.NetworkFailure(error)
        } else {
            DataTransferError.ResolvedNetworkFailure(resolvedError)
        }
    }

    /**
     * HTML을 JSON으로 변환하여 디코딩
     * Convert HTML to JSON and decode
     *
     * iOS의 htmlDecode와 동일한 로직
     * www.nifs.go.kr에서 HTML 내부의 JavaScript 데이터를 추출
     */
    private fun <T> htmlDecode(data: ByteArray?, type: Class<T>): Result<T> {
        if (data == null) {
            return Result.failure(DataTransferError.NoResponse)
        }

        return try {
            val dataString = String(data, Charsets.UTF_8)

            // "setGridData(" 와 " );" 사이의 데이터 추출
            val startMarker = "setGridData("
            val endMarker = " );"

            val startIndex = dataString.indexOf(startMarker)
            val endIndex = dataString.indexOf(endMarker, startIndex)

            if (startIndex == -1 || endIndex == -1) {
                return Result.failure(DataTransferError.NoResponse)
            }

            val extractedData = dataString.substring(
                startIndex + startMarker.length,
                endIndex
            )

            // {"list": [data]} 형태로 감싸기
            val jsonString = "{\"list\": $extractedData}"
            Log.d("DataTransferService", "Extracted JSON: $jsonString")

            val result: T = gson.fromJson(jsonString, type)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(DataTransferError.Parsing(e))
        }
    }
}

// ==================== Default Implementations ====================

/**
 * Default Data Transfer Error Logger
 */
class DefaultDataTransferErrorLogger : DataTransferErrorLogger {
    companion object {
        private const val TAG = "DataTransferService"
    }

    override fun log(error: Throwable) {
        Log.e(TAG, "-------------")
        Log.e(TAG, "Error: ${error.message}", error)
    }
}

/**
 * Default Data Transfer Error Resolver
 */
class DefaultDataTransferErrorResolver : DataTransferErrorResolver {
    override fun resolve(error: NetworkError): Throwable {
        return error
    }
}
