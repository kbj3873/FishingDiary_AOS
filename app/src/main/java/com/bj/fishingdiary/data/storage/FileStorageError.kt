package com.bj.fishingdiary.data.storage

/**
 * 파일 저장소 에러 타입
 * File Storage Error Types
 *
 * sealed class란?
 * - enum의 확장 버전으로, 각 케이스가 추가 정보를 가질 수 있음
 * - iOS Swift의 enum with associated values와 유사
 * - when 표현식에서 모든 케이스를 처리하도록 강제됨
 */
sealed class FileStorageError : Exception() {

    /**
     * 생성 에러
     * Create Error - 파일이나 디렉토리 생성 실패
     */
    object CreateError : FileStorageError() {
        override val message: String
            get() = "error: file storage [create error]"
    }

    /**
     * 읽기 에러
     * Read Error - 파일 읽기 실패
     */
    object ReadError : FileStorageError() {
        override val message: String
            get() = "error: file storage [read error]"
    }

    /**
     * 저장 에러
     * Save Error - 파일 저장 실패
     */
    object SaveError : FileStorageError() {
        override val message: String
            get() = "error: file storage [save error]"
    }

    /**
     * 삭제 에러
     * Delete Error - 파일 삭제 실패
     */
    object DeleteError : FileStorageError() {
        override val message: String
            get() = "error: file storage [delete error]"
    }
}

/**
 * 파일 생성 결과 타입
 * File Create Result Type
 *
 * Swift의 Result<Bool, FileStorageError>에 대응
 */
typealias FileCreateResult = Result<Boolean>
