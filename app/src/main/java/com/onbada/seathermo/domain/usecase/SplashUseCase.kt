package com.onbada.seathermo.domain.usecase

import com.onbada.seathermo.domain.entity.Region
import com.onbada.seathermo.domain.entity.VersionStatus
import com.onbada.seathermo.domain.repository.SplashRepository

/**
 * 스플래시 화면 UseCase 인터페이스.
 *
 * 앱 시작 시 수행되는 버전 체크와 관측소 목록 로드를 담당합니다.
 * iOS의 SplashUseCase protocol에 대응합니다.
 */
interface SplashUseCase {
    suspend fun checkVersion(appVersion: String): VersionStatus
    suspend fun fetchRegions(): List<Region>
}

/**
 * SplashUseCase 기본 구현체.
 *
 * iOS의 DefaultSplashUseCase에 대응합니다.
 */
class DefaultSplashUseCase(
    private val repository: SplashRepository
) : SplashUseCase {

    // 앱 버전 문자열을 서버에 전달하여 강제 업데이트 여부를 확인합니다.
    override suspend fun checkVersion(appVersion: String): VersionStatus {
        return repository.checkVersion(appVersion)
    }

    // 서버에서 전체 관측소 목록을 가져옵니다.
    override suspend fun fetchRegions(): List<Region> {
        return repository.fetchRegions()
    }
}
