package com.bj.fishingdiary.data.network

/**
 * 네트워크 서비스 인터페이스
 * Network Service Interface
 *
 * iOS의 DataTransferService에 대응
 * 네트워크 요청/응답을 처리하는 추상화 레이어
 */
interface NetworkService {
    // TODO: 네트워크 요청 메서드 정의
    // - HTTP GET, POST 등
    // - JSON, XML 파싱
    // - 에러 처리
}

/**
 * 네트워크 에러 타입
 * Network Error Types
 */
sealed class NetworkError : Exception() {

    /**
     * 네트워크 연결 실패
     * Connection Failed
     */
    object ConnectionError : NetworkError() {
        override val message: String
            get() = "error: network [connection error]"
    }

    /**
     * HTTP 에러 (4xx, 5xx)
     * HTTP Error
     */
    data class HttpError(val statusCode: Int) : NetworkError() {
        override val message: String
            get() = "error: network [http error: $statusCode]"
    }

    /**
     * 응답 파싱 실패
     * Response Parsing Failed
     */
    object ParsingError : NetworkError() {
        override val message: String
            get() = "error: network [parsing error]"
    }

    /**
     * 타임아웃
     * Timeout
     */
    object TimeoutError : NetworkError() {
        override val message: String
            get() = "error: network [timeout error]"
    }
}
