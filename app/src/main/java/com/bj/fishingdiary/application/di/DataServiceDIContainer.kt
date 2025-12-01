package com.bj.fishingdiary.application.di

import com.bj.fishingdiary.application.AppConfiguration
import com.bj.fishingdiary.infrastructure.network.*

/**
 * 네트워크 서비스 DI Container
 * Network Service DI Container
 *
 * iOS의 DataServiceDIContainer.swift에 대응
 * 네트워크 관련 의존성을 생성하고 관리
 */
class DataServiceDIContainer {

    /**
     * 앱 전역 설정
     */
    val appConfiguration: AppConfiguration by lazy {
        AppConfiguration()
    }

    /**
     * API Data Transfer Service (JSON)
     * Content-Type: application/json
     */
    val apiDataTransferService: DataTransferService by lazy {
        val config = ApiDataNetworkConfig(
            baseURL = appConfiguration.apiBaseURL,
            headers = mapOf(
                "Accept-Language" to "ko-KR,ko;q=0.9",
                "Content-Type" to "application/json;charset=utf-8"
            )
        )

        val apiDataNetwork = DefaultNetworkService(config = config)
        DefaultDataTransferService(networkService = apiDataNetwork)
    }

    /**
     * API XML Transfer Service (Form Data)
     * Content-Type: application/x-www-form-urlencoded
     */
    val apiXmlTransferService: DataTransferService by lazy {
        val config = ApiDataNetworkConfig(
            baseURL = appConfiguration.apiBaseURL,
            headers = mapOf(
                "Accept-Language" to "ko-KR,ko;q=0.9",
                "Content-Type" to "application/x-www-form-urlencoded"
            )
        )

        val apiDataNetwork = DefaultNetworkService(config = config)
        DefaultDataTransferService(networkService = apiDataNetwork)
    }

    /**
     * Main Scene DI Container 생성
     * Create Main Scene DI Container
     */
    fun makeMainSceneDIContainer(): MainSceneDIContainer {
        val dependencies = MainSceneDIContainer.Dependencies(
            apiDataTransferService = apiDataTransferService,
            apiXmlTransferService = apiXmlTransferService,
            appConfiguration = appConfiguration
        )
        return MainSceneDIContainer(dependencies = dependencies)
    }
}
