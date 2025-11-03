package com.bj.fishingdiary.infrastructure.network

import com.bj.fishingdiary.domain.common.Cancellable

/**
 * Repository Task
 *
 * iOS의 RepositoryTask에 대응
 * 네트워크 작업을 취소할 수 있는 Task 래퍼
 */
class RepositoryTask : Cancellable {
    var networkTask: NetworkCancellable? = null
    override var isCancelled: Boolean = false
        private set

    override fun cancel() {
        networkTask?.cancel()
        isCancelled = true
    }
}

/**
 * Network Cancellable 인터페이스
 * Network Cancellable Interface
 *
 * iOS의 NetworkCancellable protocol에 대응
 */
interface NetworkCancellable {
    fun cancel()
}
