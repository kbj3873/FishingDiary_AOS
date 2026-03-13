package com.onbada.seathermo.application.di

import android.content.Context
import com.onbada.seathermo.application.AppConfiguration
import com.onbada.seathermo.data.repository.DefaultOceanRepository
import com.onbada.seathermo.domain.repository.OceanRepository
import com.onbada.seathermo.domain.usecase.OceanUseCase
import com.onbada.seathermo.infrastructure.network.DataTransferService
import com.onbada.seathermo.presentation.main.MainViewModel

/**
 * Main Scene DI Container
 *
 * iOS의 PointSceneDIContainer.swift에 대응
 * Main 화면 관련 의존성을 생성하고 관리
 */
class MainSceneDIContainer(
    private val dependencies: Dependencies
) {

    /**
     * 외부에서 주입받는 의존성
     * Dependencies injected from outside
     */
    data class Dependencies(
        val apiDataTransferService: DataTransferService,
        val apiXmlTransferService: DataTransferService,
        val appConfiguration: AppConfiguration
    )

    // ==================== ViewModel 생성 ====================

    /**
     * MainViewModel 생성
     * Create MainViewModel
     */
    fun makeMainViewModel(context: Context): MainViewModel {
        return MainViewModel(
            context = context,
            appConfiguration = dependencies.appConfiguration,
            oceanUseCase = makeOceanUseCase()
        )
    }

    /**
     * OceanSelectViewModel 생성
     * Create OceanSelectViewModel
     */
    fun makeOceanSelectViewModel(context: Context): com.onbada.seathermo.presentation.main.OceanSelectViewModel {
        return com.onbada.seathermo.presentation.main.OceanSelectViewModel(
            context = context,
            appConfiguration = dependencies.appConfiguration,
            oceanUseCase = makeOceanUseCase()
        )
    }

    /**
     * SeaWaterTemperatureViewModel 생성
     * Create SeaWaterTemperatureViewModel
     */
    fun makeSeaWaterTemperatureViewModel(): com.onbada.seathermo.presentation.seawatertemperature.SeaWaterTemperatureViewModel {
        return com.onbada.seathermo.presentation.seawatertemperature.SeaWaterTemperatureViewModel(
            appConfiguration = dependencies.appConfiguration,
            oceanUseCase = makeOceanUseCase()
        )
    }

    // ==================== UseCase 생성 ====================

    /**
     * OceanUseCase 생성
     * Create OceanUseCase
     */
    private fun makeOceanUseCase(): OceanUseCase {
        return OceanUseCase(oceanRepository = makeOceanRepository())
    }

    // ==================== Repository 생성 ====================

    /**
     * OceanRepository 생성
     * Create OceanRepository
     */
    private fun makeOceanRepository(): OceanRepository {
        return DefaultOceanRepository(
            apiDataTransferService = dependencies.apiDataTransferService,
            apiXmlTransferService = dependencies.apiXmlTransferService
        )
    }
}
