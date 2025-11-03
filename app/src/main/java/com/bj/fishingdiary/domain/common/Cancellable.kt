package com.bj.fishingdiary.domain.common

/**
 * 취소 가능한 작업을 나타내는 인터페이스
 * Interface representing a cancellable operation
 *
 * Clean Architecture에서 비동기 작업(네트워크 호출, DB 조회 등)을 취소할 수 있게 하기 위한 추상화
 * Abstraction to allow cancelling asynchronous operations (network calls, DB queries, etc.) in Clean Architecture
 *
 * iOS의 Cancellable 프로토콜과 동일한 역할
 * Same role as iOS's Cancellable protocol
 *
 * 사용 예시:
 * Example usage:
 * ```kotlin
 * val task: Cancellable? = oceanUseCase.execute(...)
 * // 나중에 작업을 취소하고 싶을 때
 * // When you want to cancel the operation later
 * task?.cancel()
 *
 * // 취소 상태 확인
 * // Check if cancelled
 * if (task?.isCancelled == true) {
 *     // 작업이 취소됨
 * }
 * ```
 */
interface Cancellable {
    /**
     * 작업이 취소되었는지 여부
     * Whether the operation has been cancelled
     */
    val isCancelled: Boolean

    /**
     * 진행 중인 작업을 취소합니다
     * Cancels the ongoing operation
     */
    fun cancel()
}
