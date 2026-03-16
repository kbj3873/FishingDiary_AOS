package com.onbada.seathermo.data.repository

import com.onbada.seathermo.data.network.APIEndpoints
import com.onbada.seathermo.data.network.datamapping.VersionCheckRequestDTO
import com.onbada.seathermo.domain.entity.Region
import com.onbada.seathermo.domain.entity.VersionStatus
import com.onbada.seathermo.domain.repository.SplashRepository
import com.onbada.seathermo.infrastructure.network.NetworkService
import com.onbada.seathermo.infrastructure.network.request

/**
 * SplashRepository 구현체.
 *
 * iOS의 DefaultSplashRepository에 대응합니다.
 */
class DefaultSplashRepository(
    private val networkService: NetworkService
) : SplashRepository {

    /**
     * 앱 버전 상태를 서버에서 확인합니다.
     */
    override suspend fun checkVersion(appVersion: String): VersionStatus {
        val requestDTO = VersionCheckRequestDTO(appVersion = appVersion)
        val endpoint = APIEndpoints.postVersionCheck(
            baseURL = networkService.baseURL,
            requestDTO = requestDTO
        )
        val responseDTO = networkService.request(endpoint)
        return responseDTO.toDomain()
    }

    /**
     * 전체 관측소 목록을 서버에서 가져옵니다.
     */
    override suspend fun fetchRegions(): List<Region> {
        val endpoint = APIEndpoints.postRegions(baseURL = networkService.baseURL)
        val responseDTO = networkService.request(endpoint)

        // [개념] ?: return emptyList()는 guard let ... else { return [] }와 동일한 패턴입니다.
        //        data가 null이면 빈 리스트를 즉시 반환합니다.
        val data = responseDTO.data ?: return emptyList()
        return data.map { it.toDomain() }
    }
}
