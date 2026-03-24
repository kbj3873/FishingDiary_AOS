package com.onbada.seathermo.application.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.onbada.seathermo.application.AppConfiguration
import com.onbada.seathermo.presentation.currenttemperature.viewmodel.CrawlingCurrentTemperatureViewModel
import com.onbada.seathermo.presentation.currenttemperature.viewmodel.CrawlingOceanSelectViewModel
import com.onbada.seathermo.presentation.currenttemperature.viewmodel.CurrentTemperatureViewModel
import com.onbada.seathermo.presentation.currenttemperature.viewmodel.OceanSelectViewModel
import com.onbada.seathermo.presentation.onboarding.viewmodel.OnboardingViewModel
import com.onbada.seathermo.domain.entity.Region
import com.onbada.seathermo.presentation.seaanalysis.viewmodel.SeaAnalysisDetailViewModel
import com.onbada.seathermo.presentation.seaanalysis.viewmodel.SeaRegionListViewModel
import com.onbada.seathermo.presentation.fishingrecord.viewmodel.FishingRecordViewModel
import com.onbada.seathermo.presentation.setting.viewmodel.SettingViewModel
import com.onbada.seathermo.presentation.splash.viewmodel.SplashViewModel
import com.onbada.seathermo.data.repository.DefaultFishingRecordRepository
import com.onbada.seathermo.data.repository.DefaultOceanRepository
import com.onbada.seathermo.data.repository.DefaultSplashRepository
import com.onbada.seathermo.data.storage.AppDatabase
import com.onbada.seathermo.domain.repository.FishingRecordRepository
import com.onbada.seathermo.domain.repository.OceanRepository
import com.onbada.seathermo.domain.repository.SplashRepository
import com.onbada.seathermo.domain.usecase.DefaultFishingRecordUseCase
import com.onbada.seathermo.domain.usecase.DefaultOceanUseCase
import com.onbada.seathermo.domain.usecase.DefaultSplashUseCase
import com.onbada.seathermo.domain.usecase.FishingRecordUseCase
import com.onbada.seathermo.domain.usecase.OceanUseCase
import com.onbada.seathermo.domain.usecase.SplashUseCase
import com.onbada.seathermo.infrastructure.network.DefaultNetworkService
import com.onbada.seathermo.infrastructure.network.NetworkService

/**
 * 애플리케이션 전역 의존성 주입 컨테이너.
 *
 * [개념] 수동 DI(Dependency Injection) 컨테이너입니다.
 *        Hilt/Dagger 같은 DI 프레임워크 없이 의존성을 직접 생성하고 주입합니다.
 *        팩토리 메서드 패턴으로 각 레이어의 객체를 생성합니다.
 *
 * [개념] by lazy { }는 해당 프로퍼티를 처음 사용할 때만 초기화하는 지연 초기화 패턴입니다.
 *        Swift의 lazy var와 동일한 개념입니다.
 *        네트워크 서비스처럼 무거운 객체는 실제 필요한 시점에 한 번만 생성합니다.
 *
 * iOS의 ApplicationDIContainer.swift에 대응합니다.
 */
class ApplicationDIContainer(private val context: Context) {

    // ==================== Configuration ====================

    // 앱 전역 설정 (API 키, Base URL 등 BuildConfig 래퍼)
    // iOS의 lazy var appConfiguration = AppConfiguration()에 대응합니다.
    val appConfiguration: AppConfiguration by lazy {
        AppConfiguration()
    }

    // ==================== Network ====================

    /**
     * NIFS Open API용 네트워크 서비스.
     *
     * 국립수산과학원(NIFS) 해양 데이터 API에 접속합니다.
     * iOS의 lazy var apiDataTransferService: NetworkService에 대응합니다.
     */
    val apiDataTransferService: NetworkService by lazy {
        DefaultNetworkService(baseURL = appConfiguration.apiNifsURL)
    }

    /**
     * 온바다 자체 서버용 네트워크 서비스.
     *
     * 앱 버전 체크, 관측소 목록 등 자체 API에 접속합니다.
     * iOS의 lazy var seaThermoTransferService: NetworkService에 대응합니다.
     */
    val seaThermoTransferService: NetworkService by lazy {
        DefaultNetworkService(baseURL = appConfiguration.apiOnbadaURL)
    }

    // ==================== Database ====================

    // [개념] Room DB는 앱 전체에서 하나의 인스턴스를 공유하는 싱글턴입니다.
    //        iOS의 RealmManager.shared에 대응합니다.
    private val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    // ==================== Make ViewModel ====================
    // TODO: Compose Screen 구현 시 ViewModel Factory 메서드를 추가합니다.
    //
    // iOS의 extension ApplicationDIContainer - make view model 섹션에 대응합니다.
    //
    //
    fun makeCurrentTemperatureViewModelFactory(): ViewModelProvider.Factory {
        return CurrentTemperatureViewModel.provideFactory(
            application = context.applicationContext as android.app.Application,
            appConfiguration = appConfiguration,
            oceanUseCase = makeOceanUseCase()
        )
    }

    fun makeCrawlingCurrentTemperatureViewModelFactory(): ViewModelProvider.Factory {
        return CrawlingCurrentTemperatureViewModel.provideFactory(
            application = context.applicationContext as android.app.Application,
            oceanUseCase = makeOceanUseCase()
        )
    }

    fun makeOceanSelectViewModelFactory(): ViewModelProvider.Factory {
        return OceanSelectViewModel.provideFactory(
            application = context.applicationContext as android.app.Application,
            appConfiguration = appConfiguration,
            oceanUseCase = makeOceanUseCase()
        )
    }

    fun makeCrawlingOceanSelectViewModelFactory(): ViewModelProvider.Factory {
        return CrawlingOceanSelectViewModel.provideFactory(
            application = context.applicationContext as android.app.Application
        )
    }

    fun makeSplashViewModelFactory(): ViewModelProvider.Factory {
        return SplashViewModel.provideFactory(
            application = context.applicationContext as android.app.Application,
            splashUseCase = makeSplashUseCase()
        )
    }
    fun makeOnboardingViewModelFactory(): ViewModelProvider.Factory {
        return OnboardingViewModel.provideFactory(
            application = context.applicationContext as android.app.Application
        )
    }

    fun makeSeaRegionListViewModelFactory(seaAreaId: String): ViewModelProvider.Factory {
        return SeaRegionListViewModel.provideFactory(
            application = context.applicationContext as android.app.Application,
            seaAreaId = seaAreaId
        )
    }

    fun makeSettingViewModelFactory(): ViewModelProvider.Factory {
        return SettingViewModel.provideFactory()
    }

    fun makeSeaAnalysisDetailViewModelFactory(region: Region): ViewModelProvider.Factory {
        return SeaAnalysisDetailViewModel.provideFactory(
            oceanUseCase = makeOceanUseCase(),
            region = region
        )
    }

    fun makeFishingRecordViewModelFactory(): ViewModelProvider.Factory {
        return FishingRecordViewModel.provideFactory(
            application = context.applicationContext as android.app.Application,
            useCase = makeFishingRecordUseCase()
        )
    }

    // ==================== Make UseCase ====================

    /**
     * OceanUseCase 생성 팩토리.
     *
     * [개념] 팩토리 메서드 패턴: 객체 생성 로직을 한 곳에서 관리합니다.
     *        호출 측은 내부 의존성(Repository)의 존재를 몰라도 됩니다.
     *
     * iOS의 func makeOceanUseCase() -> OceanUseCase에 대응합니다.
     */
    fun makeOceanUseCase(): OceanUseCase {
        return DefaultOceanUseCase(oceanRepository = makeOceanRepository())
    }

    /**
     * SplashUseCase 생성 팩토리.
     *
     * iOS의 func makeSplashUseCase() -> SplashUseCase에 대응합니다.
     */
    fun makeSplashUseCase(): SplashUseCase {
        return DefaultSplashUseCase(repository = makeSplashRepository())
    }

    /**
     * FishingRecordUseCase 생성 팩토리.
     *
     * iOS의 func makeFishingRecordUseCase() -> FishingRecordUseCase에 대응합니다.
     */
    fun makeFishingRecordUseCase(): FishingRecordUseCase {
        return DefaultFishingRecordUseCase(repository = makeFishingRecordRepository())
    }

    // ==================== Make Data Repository ====================

    /**
     * OceanRepository 생성 팩토리.
     *
     * NIFS API 네트워크 서비스를 주입합니다.
     * iOS의 func makeOceanRepository() -> OceanRepository에 대응합니다.
     */
    private fun makeOceanRepository(): OceanRepository {
        return DefaultOceanRepository(networkService = apiDataTransferService)
    }

    /**
     * SplashRepository 생성 팩토리.
     *
     * 온바다 서버 네트워크 서비스를 주입합니다.
     * iOS의 func makeSplashRepository() -> SplashRepository에 대응합니다.
     */
    private fun makeSplashRepository(): SplashRepository {
        return DefaultSplashRepository(networkService = seaThermoTransferService)
    }

    /**
     * FishingRecordRepository 생성 팩토리.
     *
     * Room DAO와 앱 Context를 주입합니다.
     * iOS의 func makeFishingRecordRepository() -> FishingRecordRepository에 대응합니다.
     */
    private fun makeFishingRecordRepository(): FishingRecordRepository {
        return DefaultFishingRecordRepository(
            dao = database.fishingRecordDao(),
            context = context
        )
    }
}
