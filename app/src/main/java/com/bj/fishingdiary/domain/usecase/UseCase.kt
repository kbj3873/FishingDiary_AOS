package com.bj.fishingdiary.domain.usecase

import com.bj.fishingdiary.domain.common.Cancellable

/**
 * UseCase 기본 인터페이스
 * Base UseCase interface
 *
 * Clean Architecture의 UseCase 레이어를 나타내는 기본 인터페이스
 * Base interface representing the UseCase layer in Clean Architecture
 *
 * UseCase란?
 * - 애플리케이션의 비즈니스 로직을 캡슐화하는 단위
 * - 하나의 UseCase는 하나의 특정 작업을 수행 (Single Responsibility Principle)
 * - Repository를 통해 데이터를 가져오고, 비즈니스 규칙을 적용
 *
 * What is a UseCase?
 * - A unit that encapsulates the business logic of the application
 * - One UseCase performs one specific task (Single Responsibility Principle)
 * - Fetches data through Repository and applies business rules
 *
 * 예시:
 * Example:
 * - OceanUseCase: 해양 데이터를 조회하는 비즈니스 로직
 * - PointMapUseCase: 낚시 포인트 지도 데이터를 처리하는 로직
 */
interface UseCase {
    /**
     * UseCase 실행을 시작합니다
     * Starts the UseCase execution
     *
     * @return 취소 가능한 작업 객체 (null일 수 있음)
     *         Cancellable operation object (can be null)
     */
    fun start(): Cancellable?
}
