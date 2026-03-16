# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 저장소에서 작업할 때 참고하는 가이드 문서입니다.

## 관련 문서

| 문서 | 용도 | 언제 참조 |
|-----|------|----------|
| [기능 개발 워크플로우](.agent/workflows/figma_to_compose_workflow.md) | Standard Workflow | 디자인 변환 및 기능 구현 시작 시 **필수** |
| [작업 로그 가이드](.agent/workflows/work_log.md) | Notion Work Log | 일일 작업 로그 기록 시 |

## 프로젝트 개요

**온바다(SeaThermo)**는 낚시 활동을 기록하고 추적하는 Android 애플리케이션으로, 해양 데이터 통합 기능을 제공합니다. 한국 해양 API에서 실시간 해수 온도 정보를 가져오고, GPS 기반 낚시 위치 추적 및 듀얼 맵 지원(Google Maps와 Kakao Maps)을 제공합니다.

- **플랫폼:** Android API 24+ (Android 7.0)
- **언어:** Kotlin
- **UI 프레임워크:** Jetpack Compose
- **아키텍처:** Clean Architecture + MVVM
- **의존성 관리:** Gradle (Version Catalog)
- **패키지명:** `com.onbada.seathermo`

## 레거시 정책 (Legacy Policy)

현재 앱은 프로토타입 단계로, 기존 XML 기반 Activity/Adapter/View 코드는 모두 **삭제 대상 레거시**입니다.

- **신규 기능은 반드시 Jetpack Compose로 구현**합니다.
- Compose 화면 구현 완료 후 대응되는 레거시 파일을 삭제합니다.
- 레거시 파일은 절대 수정하지 않습니다.

### 네이밍 충돌 해결 규칙

신규 Compose 코드에서 정의할 파일명, 클래스명, 메서드명, 모델명이 **레거시 코드와 중복**될 경우, 레거시 쪽 이름에 접두사를 붙여 구분합니다.

- **`Legacy`** 접두사: 클래스/파일 단위 충돌 시 우선 사용 (예: `LegacyMainViewModel`, `LegacyOceanSelectActivity`)
- **`Dummy`** 접두사: 더 이상 실제 동작이 필요 없는 껍데기 코드에 사용 (예: `DummyTemperatureAdapter`)
- **`Temp`** 접두사: 임시로 공존해야 하는 과도기적 코드에 사용 (예: `TempPointMapActivity`)

> **원칙**: 신규 코드가 올바른 이름을 가져가고, 레거시 코드가 이름을 양보합니다.
> 이름 변경 후에도 레거시 파일 내부 코드는 수정하지 않습니다.

**예시:**
```
// 충돌 상황: 신규 Compose 화면과 레거시 Activity가 같은 이름을 사용하려 할 때
// Before
presentation/main/MainViewModel.kt  ← 레거시 (삭제 예정)
// After (충돌 해결)
presentation/main/LegacyMainViewModel.kt  ← 레거시 (삭제 대기)
presentation/main/MainViewModel.kt        ← 신규 Compose용 ViewModel
```

| 경로 | 설명 | 처리 방향 |
|-----|-----|---------|
| `presentation/main/MainHomeActivity.kt` 등 | XML 기반 Activity | Compose Screen으로 대체 후 삭제 |
| `presentation/*/.*Adapter.kt` | RecyclerView Adapter | LazyColumn으로 대체 후 삭제 |
| `presentation/seawatertemperature/TemperatureLineGraphView.kt` | Custom View (Canvas) | Compose Canvas로 대체 후 삭제 |
| `presentation/track/`, `presentation/point/` | 포인트/트랙 관련 화면 | 신규 Compose 모듈로 재개발 후 삭제 |

## 빌드 명령어

```bash
# 디버그 빌드
./gradlew assembleDebug

# 릴리즈 빌드
./gradlew assembleRelease

# 클린 빌드
./gradlew clean assembleDebug

# 단위 테스트
./gradlew test

# 설치 (연결된 디바이스/에뮬레이터)
./gradlew installDebug
```

**API 키 설정:** 프로젝트 루트의 `local.properties`에 아래 키를 추가합니다.
```properties
RISA_API_KEY=...
API_BASE_URL=...
GOOGLE_MAPS_KEY=...
KAKAO_APP_KEY=...
```

## 애플리케이션 시작 흐름

```
FDApplication (Application 서브클래스)
  → AppDIContainer (전역 싱글턴 DI)
      → MainActivity (단일 Activity)
          → NavHost (Compose Navigation)
              → SplashScreen
              → OnboardingScreen
              → MainTabScreen (BottomNavigation 기반 탭 구조)
```

**핵심 파일:**
- `FDApplication.kt`: Application 진입점, 전역 초기화 (Kakao SDK 등)
- `MainActivity.kt`: 단일 Activity, NavHost 호스팅
- `MainTabScreen.kt`: 탭 기반 메인 화면 구성

## Jetpack Compose 아키텍처

### 화면 구성

| 영역 | 화면 | Composable | 통합 방식 | 상태 |
|---|---|---|---|---|
| **Main** | 메인 탭 | `MainTabScreen` | NavHost 최상단 | Active |
| **Main** | 메인 홈 | `CurrentTemperatureScreen` | BottomNav Item | Active |
| **Main** | 해양 선택 | `OceanSelectScreen` | Navigation Route | Active |
| **SeaAnalysis** | 수온 분석 홈 | `SeaAnalysisScreen` | BottomNav Item | Active |
| **SeaAnalysis** | 수온 상세 | `SeaAnalysisDetailScreen` | Navigation Route | Active |
| **FishingRecord** | 낚시 기록 | `FishingRecordScreen` | BottomNav Item | Active |
| **History** | 조과 기록 목록 | `HistoryScreen` | BottomNav Item | Active |
| **History** | 조과 상세 | `HistoryDetailScreen` | Navigation Route | Active |
| **Setting** | 설정 | `SettingScreen` | BottomNav Item | Active |

### Navigation 구조

```kotlin
// MainActivity.kt
NavHost(navController = navController, startDestination = "splash") {
    composable("splash") { SplashScreen(navController) }
    composable("onboarding") { OnboardingScreen(navController) }
    composable("main") { MainTabScreen(navController) }
}
```

## Clean Architecture 레이어

### 1. Domain 레이어 (`domain/`)

비즈니스 로직과 엔티티를 포함하는 가장 내부 레이어로, Android 프레임워크와 독립적입니다.

**구조:**
- `entity/` - 순수 비즈니스 모델 (Ocean, PointMap, SeaInfo 등)
- `repository/` - Repository 인터페이스 (데이터 접근 추상화)
- `usecase/` - 비즈니스 로직 오케스트레이터

**주요 패턴:**
- 모든 Repository는 인터페이스 기반 (예: `OceanRepository`)
- UseCase는 단일 책임 원칙에 따라 비즈니스 흐름을 오케스트레이션합니다.
- 비동기 작업은 Kotlin Coroutines(`suspend fun`) 표준 패턴을 채택했습니다.

**예시:**
```kotlin
// domain/repository/OceanRepository.kt
interface OceanRepository {
    suspend fun fetchTemperatureList(query: OceanQuery): List<CurrentTemperature>
}

// domain/usecase/OceanUseCase.kt
class OceanUseCase(private val oceanRepository: OceanRepository) {
    suspend fun executeRisaList(query: OceanQuery): List<CurrentTemperature> {
        return oceanRepository.fetchTemperatureList(query)
    }
}
```

### 2. Data 레이어 (`data/`)

Repository 구현체와 데이터 소스(네트워크, 로컬 저장소) 구현부입니다.

**구조:**
- `repository/` - `Default` 접두사를 가진 구체적인 구현체 (예: `DefaultOceanRepository`)
- `network/` - API 엔드포인트 및 DTO 매핑
- `storage/` - 로컬 Room DB 기반 저장소

**주요 패턴:**
- Repository 구현체는 `NetworkService` 주입 (단일 네트워크 서비스)
- DTO (Data Transfer Objects)를 도메인 엔티티로 매핑
- Kotlin Coroutines 활용 통일

**예시:**
```kotlin
// data/repository/DefaultOceanRepository.kt
class DefaultOceanRepository(
    private val networkService: NetworkService
) : OceanRepository {
    override suspend fun fetchTemperatureList(query: OceanQuery): List<CurrentTemperature> {
        val requestDTO = OceanRequestDTO(query)
        val endpoint = APIEndpoints.getRisaJson(requestDTO)
        val responseDTO: OceanResponseDTO = networkService.request(endpoint)

        if (responseDTO.header.resultCode != "00") {
            throw NetworkError.ApiError(responseDTO.header.resultCode)
        }

        return responseDTO.body.item?.map { it.toDomain() } ?: emptyList()
    }
}
```

### 3. Presentation 레이어 (`presentation/`)

Jetpack Compose + MVVM 기반의 UI 레이어

**구조:**
- `maintab/`
  - `MainTabScreen.kt` - 탭 기반 앱 라우팅 및 BottomNavigation 구성

- `currenttemperature/`
  - `screen/CurrentTemperatureScreen.kt` - 현재 수온 상태 정보 홈 화면
  - `screen/OceanSelectScreen.kt` - 즐겨찾기 해양 측정소 선택 화면
  - `viewmodel/CurrentTemperatureViewModel.kt`

- `seaanalysis/`
  - `SeaAnalysisScreen.kt` - 수온 분석 홈 화면
  - `SeaAnalysisDetailScreen.kt` - 수온 분석 상세 차트 화면

- `fishingrecord/`
  - `screen/FishingRecordScreen.kt` - 낚시 기록 등록 및 지도 기반 기록
  - `screen/components/KakaoMapView.kt` - Kakao Maps AndroidView 래퍼
  - `screen/components/GoogleMapView.kt` - Google Maps Compose 래퍼

- `history/`
  - `HistoryScreen.kt` - 조과 기록 목록 조회
  - `HistoryDetailScreen.kt` - 조과 상세 정보
  - `HistoryImageViewer.kt` - 풀스크린 사진 모아보기

- `setting/`
  - `SettingScreen.kt` - 설정 메뉴 관리 및 WebView 라우팅

**MVVM 구현 패턴:**
- **ViewModel 정의**: `ViewModel()`을 상속하고 `viewModelScope`를 사용하여 UI 상태 업데이트가 안전하게 처리되도록 합니다.
- **상태 관리**: `MutableStateFlow`를 `private`으로 선언하고 `StateFlow`로 노출합니다.
- **의존성 주입**: ViewModel 생성은 `AppDIContainer`의 Factory 메서드를 통해 주입받습니다.
- **비동기 처리**: UseCase 로직은 `LaunchedEffect` 내부 또는 ViewModel의 `viewModelScope.launch`에서 호출합니다.

```kotlin
class CurrentTemperatureViewModel(
    private val oceanUseCase: OceanUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CurrentTemperatureUiState())
    val uiState: StateFlow<CurrentTemperatureUiState> = _uiState.asStateFlow()

    fun fetchCurrentTemperature() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val result = oceanUseCase.executeRisaList(OceanQuery(...))
                _uiState.update { it.copy(temperatureList = result, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}

data class CurrentTemperatureUiState(
    val temperatureList: List<CurrentTemperature> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
```

## 의존성 주입 (Dependency Injection)

### DI 컨테이너 구조

```
FDApplication
  └─ AppDIContainer (전역 싱글턴)
      └─ DataServiceDIContainer (데이터 레이어 의존성)
          ├─ AppConfiguration (BuildConfig API 로드)
          ├─ NetworkService (단일 네트워크 서비스)
          ├─ RoomDatabase (로컬 DB)
          ├─ Repositories (OceanRepository 등)
          ├─ UseCases (OceanUseCase 등)
          └─ ViewModels (모든 ViewModel Factory)
```

### ViewModel Factory 패턴

Hilt 없이 수동 DI를 사용하며, `ViewModelProvider.Factory`를 통해 ViewModel에 의존성을 주입합니다.

```kotlin
// application/di/DataServiceDIContainer.kt
class DataServiceDIContainer(private val appConfiguration: AppConfiguration) {

    private val networkService: NetworkService by lazy {
        DefaultNetworkService(appConfiguration)
    }

    fun makeCurrentTemperatureViewModel(): CurrentTemperatureViewModel {
        return CurrentTemperatureViewModel(makeOceanUseCase())
    }

    private fun makeOceanUseCase(): OceanUseCase {
        return OceanUseCase(makeOceanRepository())
    }

    private fun makeOceanRepository(): OceanRepository {
        return DefaultOceanRepository(networkService)
    }
}
```

### Compose에서 ViewModel 사용

```kotlin
// Screen에서의 사용 예시
@Composable
fun MainTabScreen(diContainer: DataServiceDIContainer) {
    val viewModel = remember { diContainer.makeCurrentTemperatureViewModel() }
    CurrentTemperatureScreen(viewModel = viewModel)
}

// 또는 CompositionLocal로 DI 컨테이너 전달
val LocalDIContainer = compositionLocalOf<DataServiceDIContainer> {
    error("DIContainer not provided")
}
```

## Infrastructure 레이어 (`infrastructure/network/`)

저수준 네트워킹 추상화:
- `DefaultNetworkService.kt` - OkHttp 래퍼 (단일 네트워크 서비스 역할)
- `Endpoint.kt` - 제네릭 엔드포인트 프로토콜
- Kotlin Coroutines(`suspend fun`) 기반 전면적인 비동기 처리

**API 통합 (Base URL):**
- NIFS API (국립수산과학원 해양 데이터)
- Onbada API (자체 서비스 수온 데이터)
- Kakao Maps SDK (지도 시각화)

**API 키 및 URL 관리:**
`local.properties`에 저장되며 `BuildConfig`를 통해 로드, `AppConfiguration`이 래핑:
- `RISA_API_KEY`
- `API_BASE_URL`
- `GOOGLE_MAPS_KEY`
- `KAKAO_APP_KEY`

## Managers (`managers/`)

애플리케이션 전역 매니저:
- `FDAppManager` - 앱 상태 관리 (지도 타입 선택, 상수, 초기화)
- `FDLocationManager` - GPS 위치 추적 및 권한 관리

## 네이밍 규칙

- **인터페이스:** 목적에 따라 접미사 (`Repository`, `UseCase`)
- **구현체:** `Default` 접두사 (예: `DefaultOceanRepository`)
- **DTO:** `DTO` 접미사 (예: `OceanRequestDTO`, `OceanResponseDTO`)
- **요청/응답:** `Query` 또는 `Response` 접미사
- **ViewModel:** `ViewModel` 접미사
- **Compose Screen:** `Screen` 접미사 (예: `MainTabScreen`, `OceanSelectScreen`)
- **Compose 재사용 컴포넌트:** `View` 접미사 (예: `KakaoMapView`, `TemperatureCard`)
- **DI 컨테이너:** `DIContainer` 접미사 (예: `DataServiceDIContainer`)
- **전역 베이스 클래스:** `FD` 접두사 (예: `FDAppManager`, `FDUserDefaults`)
- **UiState:** `UiState` 접미사 (예: `CurrentTemperatureUiState`)

## 코드 구조 규칙

### 레이어 의존성 (단방향)

```
Presentation (Composable Screen + ViewModel)
    ↓
Domain (UseCase + Repository Interface + Entity)
    ↓
Data (Repository Implementation + DTO)
    ↓
Infrastructure (Network / Storage)
```

- Domain 레이어는 절대 Data나 Presentation에 의존하지 않음 (엔티티 중심)
- Domain에서는 인터페이스를 정의하고, Data 계층에서 그 인터페이스를 구현

### Repository 패턴

1. `domain/repository/`에 `Repository` 인터페이스 정의
2. `data/repository/`에 `Default` 접두사로 구현체 작성
3. 네트워크 통신 구현체는 의존성으로 `NetworkService`를 주입받아 사용

### DTO 매핑

- DTO 파일은 `data/network/datamapping/` 폴더에 생성
- `toDomain()` 확장 함수를 통해 DTO를 순수 도메인 엔티티로 변환
- 도메인 엔티티 코드 내부에는 외부 프레임워크나 API 스펙 변경의 여파가 닿지 않아야 함

### 비동기 작업

모든 네트워크/DB I/O는 Kotlin Coroutines 기반의 `suspend fun`으로 작성합니다. UI 업데이트는 `viewModelScope`(Main Dispatcher 기본값)로 보장합니다.

```kotlin
fun fetchData() {
    viewModelScope.launch {
        try {
            val result = repository.fetch() // suspend fun
            _uiState.update { it.copy(items = result) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message) }
        }
    }
}
```

### Compose State 관리 패턴

```kotlin
// ViewModel
class MyViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MyUiState())
    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()
}

// Screen
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }
}
```

## 일반적인 개발 작업

### 새로운 Compose 화면 추가 가이드

1. `presentation/` 영역에 Screen Composable 생성 (`*Screen.kt`)
   ```kotlin
   @Composable
   fun MyFeatureScreen(
       viewModel: MyFeatureViewModel,
       onNavigateBack: () -> Unit = {}
   ) {
       val uiState by viewModel.uiState.collectAsStateWithLifecycle()

       LaunchedEffect(Unit) {
           viewModel.loadData()
       }

       Scaffold(
           topBar = { /* TopAppBar */ }
       ) { paddingValues ->
           Box(modifier = Modifier.padding(paddingValues)) {
               // Content
           }
       }
   }
   ```

2. ViewModel 생성 (`ViewModel()` 상속, `viewModelScope` 활용)
   ```kotlin
   class MyFeatureViewModel(
       private val useCase: MyFeatureUseCase
   ) : ViewModel() {
       private val _uiState = MutableStateFlow(MyFeatureUiState())
       val uiState: StateFlow<MyFeatureUiState> = _uiState.asStateFlow()

       fun loadData() {
           viewModelScope.launch {
               // 비동기 통신 로직
           }
       }
   }
   ```

3. `DataServiceDIContainer`에 ViewModel Factory 메서드 추가
   ```kotlin
   fun makeMyFeatureViewModel(): MyFeatureViewModel {
       return MyFeatureViewModel(makeMyFeatureUseCase())
   }
   ```

4. Navigation Route 등록
   ```kotlin
   composable("my_feature") {
       MyFeatureScreen(
           viewModel = diContainer.makeMyFeatureViewModel(),
           onNavigateBack = { navController.popBackStack() }
       )
   }
   ```

### Kakao/Google Maps를 Compose에 통합

Kakao Maps SDK 및 Google Maps는 AndroidView를 통해 Compose 환경에 래핑합니다.

**AndroidView 래핑 예시 (Kakao Map):**
```kotlin
@Composable
fun KakaoMapView(
    modifier: Modifier = Modifier,
    onMapReady: (KakaoMap) -> Unit
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).also { mapView ->
                mapView.start(
                    object : MapLifeCycleCallback() {
                        override fun onMapDestroy() {}
                        override fun onMapError(e: Exception) {}
                    },
                    object : KakaoMapReadyCallback() {
                        override fun onMapReady(kakaoMap: KakaoMap) {
                            onMapReady(kakaoMap)
                        }
                    }
                )
            }
        },
        update = { /* 상태 변화 시 맵 업데이트 */ }
    )
}
```

### 새로운 도메인 엔티티 추가

1. `domain/entity/`에 엔티티 data class 생성
2. `domain/repository/`에 Repository 인터페이스 정의
3. `domain/usecase/`에 UseCase 생성
4. `data/repository/`에 `Default` 접두사 구현체 작성
5. `data/network/datamapping/`에 DTO 및 `toDomain()` 확장 함수 작성
6. `DataServiceDIContainer`에서 의존성 연결

### 새로운 API 엔드포인트 추가 가이드

1. `data/network/APIEndpoints.kt`에 엔드포인트 팩토리 정의
2. `data/network/datamapping/` 하위에 Request/Response DTO 생성
3. DTO에 `toDomain()` 확장 함수 추가

## Gradle 설정 (Compose 의존성)

`app/build.gradle.kts`에 반드시 포함되어야 할 Compose 설정:

```kotlin
android {
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.x"
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.xx.xx")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose")
    implementation("androidx.lifecycle:lifecycle-runtime-compose") // collectAsStateWithLifecycle
    implementation("androidx.navigation:navigation-compose")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

## 주요 의존성 (Dependencies)

- **Jetpack Compose BOM** - UI 프레임워크
- **Navigation Compose** - 화면 전환 라우팅
- **KakaoMaps SDK (Android)** - 낚시 포인트 및 지도 시각화 (한국 맵 전용)
- **Google Maps Compose** - 구글 지도 시각화
- **Room** - 로컬 데이터베이스 (낚시 기록 저장)
- **OkHttp** - HTTP 네트워크 클라이언트
- **Kotlin Coroutines** - 비동기 처리
- **Firebase** - Analytics, Crashlytics 등 앱 관제 (적용 시점)
- **기본 AndroidX 프레임워크:** Lifecycle, ViewModel, Activity, Foundation

## 성능 최적화

### 상태 관리 (Recomposition 방지)
- **`remember` 활용**: 재계산이 필요 없는 객체는 `remember { }` 로 캐싱
- **`key` 파라미터**: 리스트 아이템은 `key = { item.id }` 로 안정적인 identity 보장
- **`derivedStateOf`**: 다른 State로부터 파생된 상태는 `derivedStateOf { }` 사용

### 맵 컴포넌트 Lifecycle 관리
- `AndroidView`로 래핑된 맵 컴포넌트는 `DisposableEffect`로 명시적 해제 처리
```kotlin
DisposableEffect(Unit) {
    onDispose {
        mapView.finish() // Kakao MapView 해제
    }
}
```

## 주석 작성 원칙 (학습용 주석)

이 프로젝트는 Kotlin 및 Android 개발을 학습하면서 진행합니다. 따라서 **모든 코드에 개념 설명 주석을 함께 작성**합니다.

### 주석의 두 가지 역할

1. **"무엇을 하는가" (What)**: 이 코드가 수행하는 동작 설명
2. **"왜 이렇게 쓰는가" (Why/Concept)**: 사용된 Kotlin/Android 문법 또는 패턴의 개념 설명

### 주석 작성 스타일 예시

#### 클래스 레벨 주석
```kotlin
/**
 * 현재 수온 정보를 관리하는 ViewModel.
 *
 * [개념] ViewModel은 화면(Activity/Composable)이 사라져도 데이터를 유지하는 컨테이너입니다.
 * 화면 회전 등 구성 변경(Configuration Change) 시에도 데이터가 초기화되지 않습니다.
 * Android Jetpack의 lifecycle-viewmodel 라이브러리가 제공합니다.
 */
class CurrentTemperatureViewModel(
    private val oceanUseCase: OceanUseCase
) : ViewModel()
```

#### 프로퍼티 주석
```kotlin
// UI 상태를 외부에 노출하는 읽기 전용 스트림.
// [개념] StateFlow는 항상 최신 값을 보유하는 '상태 홀더'입니다.
//        MutableStateFlow로 내부에서 값을 변경하고, StateFlow로 외부에 읽기 전용 노출하는 것이
//        캡슐화(Encapsulation) 패턴입니다. Swift의 private(set) var와 동일한 의도입니다.
private val _uiState = MutableStateFlow(CurrentTemperatureUiState())
val uiState: StateFlow<CurrentTemperatureUiState> = _uiState.asStateFlow()
```

#### 함수 레벨 주석
```kotlin
// 수온 데이터를 API로부터 불러와 uiState를 갱신합니다.
// [개념] viewModelScope.launch는 ViewModel의 생명주기에 묶인 코루틴을 시작합니다.
//        ViewModel이 소멸되면 코루틴도 자동으로 취소되어 메모리 누수를 방지합니다.
//        Swift의 Task { } 블록과 동일한 역할입니다.
fun fetchCurrentTemperature() {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        // [개념] try-catch는 Swift의 do { try } catch { }와 동일합니다.
        //        suspend 함수에서 발생한 예외를 잡아 처리합니다.
        try {
            val result = oceanUseCase.executeRisaList(OceanQuery())
            // [개념] _uiState.update { }는 현재 상태를 복사하여 일부만 변경하는 패턴입니다.
            //        data class의 copy() 함수를 활용하여 불변성(Immutability)을 유지합니다.
            _uiState.update { it.copy(temperatureList = result, isLoading = false) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = e.message) }
        }
    }
}
```

#### Composable 함수 주석
```kotlin
// 현재 수온 정보를 표시하는 메인 홈 화면입니다.
// [개념] @Composable 어노테이션이 붙은 함수는 UI를 '선언'합니다.
//        상태(state)가 변경되면 Compose 런타임이 해당 함수를 자동으로 재호출(Recomposition)합니다.
//        Swift의 SwiftUI View와 동일한 개념입니다.
@Composable
fun CurrentTemperatureScreen(viewModel: CurrentTemperatureViewModel) {

    // [개념] collectAsStateWithLifecycle()은 Flow를 Compose State로 변환합니다.
    //        화면이 백그라운드로 가면 자동으로 수집을 중단하여 불필요한 연산을 막습니다.
    //        Swift의 @StateObject + @Published 조합과 동일한 역할입니다.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // [개념] LaunchedEffect는 Composable이 화면에 나타날 때 한 번 코루틴을 실행합니다.
    //        key 값(여기선 Unit = 변하지 않는 값)이 바뀌면 재실행됩니다.
    //        Swift의 .task { } 모디파이어와 동일한 역할입니다.
    LaunchedEffect(Unit) {
        viewModel.fetchCurrentTemperature()
    }
}
```

#### Repository / UseCase 주석
```kotlin
// OceanRepository 인터페이스의 실제 구현체입니다.
class DefaultOceanRepository(
    // [개념] 생성자 파라미터에 val/var 없이 선언하면 클래스 내부에서만 사용하는 프로퍼티가 됩니다.
    //        private val로 선언하면 외부 접근을 막는 캡슐화가 적용됩니다.
    private val networkService: NetworkService
) : OceanRepository {

    // [개념] override는 인터페이스 또는 상위 클래스의 함수를 재정의할 때 사용합니다.
    //        suspend fun은 코루틴 안에서만 호출할 수 있는 '일시 중단 가능한 함수'입니다.
    //        Swift의 async func와 동일한 개념입니다.
    override suspend fun fetchTemperatureList(query: OceanQuery): List<CurrentTemperature> {
        val responseDTO: OceanResponseDTO = networkService.request(APIEndpoints.getRisaJson(query))
        // [개념] ?: 는 Elvis 연산자입니다. 좌변이 null이면 우변 값을 반환합니다.
        //        Swift의 ?? (nil 병합 연산자)와 동일합니다.
        return responseDTO.body.item?.map { it.toDomain() } ?: emptyList()
    }
}
```

### 주석 작성 강도 기준

| 상황 | 주석 수준 |
|---|---|
| Kotlin 고유 문법 (`?.`, `?:`, `let`, `apply`, `also` 등) | **반드시** 개념 주석 추가 |
| Compose 어노테이션 및 Side Effect (`LaunchedEffect`, `DisposableEffect` 등) | **반드시** 개념 주석 추가 |
| Android 아키텍처 컴포넌트 (`ViewModel`, `StateFlow`, `Room` 등) | **반드시** 개념 주석 추가 |
| 단순 변수 선언, 로그 출력 등 자명한 코드 | 주석 생략 가능 |

## 언어 및 소통 원칙

모든 작업 관련 문서, 사용자 소통은 **한국어**로 작성하는 것을 원칙으로 합니다.
