package com.onbada.seathermo.domain.repository

import com.onbada.seathermo.domain.entity.Region
import com.onbada.seathermo.domain.entity.VersionStatus

/**
 * 스플래시 화면용 Repository 인터페이스.
 *
 * 앱 시작 시 필요한 초기 데이터(버전 체크, 관측소 목록)를 가져옵니다.
 * iOS의 SplashRepository protocol에 대응합니다.
 */
interface SplashRepository {

    /**
     * 앱 버전 상태를 서버에서 확인합니다.
     *
     * 강제 업데이트 여부, 최신 버전 정보 등을 반환합니다.
     *
     * @param appVersion 현재 설치된 앱 버전 문자열 (예: "1.0.0").
     * @return 버전 상태 정보.
     */
    suspend fun checkVersion(appVersion: String): VersionStatus

    /**
     * 전체 관측소(Region) 목록을 가져옵니다.
     *
     * 스플래시에서 미리 로드하여 해양 선택 화면에서 사용합니다.
     *
     * @return 해역별 관측소 목록.
     */
    suspend fun fetchRegions(): List<Region>
}
