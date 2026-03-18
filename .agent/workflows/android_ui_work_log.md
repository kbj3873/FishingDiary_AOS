# SeaThermo Android UI 구현 작업 로그

> **목적:** Figma 디자인 + iOS(SwiftUI) 참고 코드를 기반으로 Android Jetpack Compose UI를 정밀하게 이식한다.

---

## 📌 핵심 참고 자료

| 자료 | 경로/위치 | 용도 |
|------|----------|------|
| iOS 소스 | `/Users/kimbyeongjoon/Documents/workspace_seathermo/SeaThermo_iOS/SeaThermo/Presentation/` | UI 구조, 로직 참고 |
| Figma | MCP 연결 (현재 선택 노드 기준 조회) | 정확한 크기·색상·간격 |
| Android ViewModel | `SeaThermo_AOS/app/src/main/.../presentation/` | 이미 완성된 비즈니스 로직 |
| Android MainActivity | `SeaThermo_AOS/app/.../MainActivity.kt` | NavHost 진입점 |

---

## 🗺️ 전체 구현 순서

```
1단계 (Foundation)   → 테마 · 디자인 토큰
2단계 (Entry Flow)   → Splash → Onboarding → MainTab 연결
3단계 (Tab Screens)  → CurrentTemperature → SeaAnalysis → FishingRecord → History → Setting
4단계 (Sub Screens)  → OceanSelect · SeaAnalysisDetail · HistoryDetail · HistoryImageViewer
5단계 (Polish)       → 공통 컴포넌트 정리 · 애니메이션 · QA
```

---

## 📊 현재 구현 상태

### ViewModel (비즈니스 로직) — 전체 완성 ✅
| 파일 | 상태 |
|------|------|
| `SplashViewModel.kt` | ✅ 완성 |
| `OnboardingViewModel.kt` | ✅ 완성 |
| `CurrentTemperatureViewModel.kt` | ✅ 완성 |
| `CrawlingCurrentTemperatureViewModel.kt` | ✅ 완성 |
| `OceanSelectViewModel.kt` | ✅ 완성 |
| `SeaAnalysisViewModel.kt` | ✅ 완성 |
| `SeaAnalysisDetailViewModel.kt` | ✅ 완성 |
| `SeaRegionListViewModel.kt` | ✅ 완성 |
| `FishingRecordViewModel.kt` | ✅ 완성 |
| `HistoryViewModel.kt` | ✅ 완성 |
| `HistoryDetailViewModel.kt` | ✅ 완성 |
| `SettingViewModel.kt` | ✅ 완성 |

### Screen (Composable UI)
| 파일 | 상태 | 비고 |
|------|------|------|
| `ui/theme/` (Color, Type, Shape) | 🟡 점진적 추가 중 | 화면 구현 시 필요한 것만 추가 |
| `SplashScreen.kt` | ✅ 완성 | |
| `OnboardingScreen.kt` | ✅ 완성 | |
| `MainTabScreen.kt` | 🟡 골격만 (Placeholder) | **현재 작업** — 탭 아이콘 연결 필요 |
| `CurrentTemperatureScreen.kt` | 🔴 미구현 | |
| `OceanSelectScreen.kt` | 🔴 미구현 | |
| `SeaAnalysisScreen.kt` | 🔴 미구현 | |
| `SeaAnalysisDetailScreen.kt` | 🔴 미구현 | |
| `FishingRecordScreen.kt` | 🔴 미구현 | |
| `HistoryScreen.kt` | 🔴 미구현 | |
| `HistoryDetailScreen.kt` | 🔴 미구현 | |
| `HistoryImageViewer.kt` | 🔴 미구현 | |
| `SettingScreen.kt` | 🔴 미구현 | |

---

## 🔨 단계별 상세 작업 계획

### 1단계: 테마 · 디자인 시스템 구축

**경로:** `app/src/main/java/com/onbada/seathermo/ui/theme/`
**iOS 참고:** `SeaThermo_iOS/SeaThermo/Common/Extentions/Color.swift`

- [ ] `Color.kt` — iOS Color.swift 기반 색상 이식 (Figma 변수 정의로 hex 추출)
- [ ] `Type.kt` — 앱 전체 타이포그래피 (iOS 폰트 스타일 → Material3 매핑)
- [ ] `Shape.kt` — 카드·버튼 모서리 반경
- [ ] `SeaThermoTheme.kt` — MaterialTheme 래퍼

---

### 2단계: 진입 플로우

#### 2-2. OnboardingScreen
**경로:** `presentation/onboarding/OnboardingScreen.kt`
**iOS 참고:** `Presentation/Onboarding/OnboardingView.swift`

- [ ] Figma Onboarding 프레임 노드 확인
- [ ] `OnboardingScreen.kt` Composable 생성
- [ ] `OnboardingViewModel` 연결
- [ ] `MainActivity.kt` NavHost `"onboarding"` route 연결

#### 2-3. MainTabScreen 완성 ← **현재 작업**
**경로:** `presentation/maintab/MainTabScreen.kt`
**iOS 참고:** `Presentation/MainTab/MainTabView.swift`

**iOS 탭 아이콘 에셋 위치:**
`SeaThermo_iOS/SeaThermo/Assets.xcassets/TabBar/tab_*.imageset/`
→ @2x → `drawable-xhdpi/`, @3x → `drawable-xxhdpi/`

**색상 (iOS 기준, 이미 하드코딩됨):**
- 활성: `#2563EB`
- 비활성: `#8E8E93`
- 탭바 배경: `white` (alpha 0.8)
- 탭 레이블 폰트: 10sp

- [x] 골격 구조 완성
- [ ] iOS 탭 아이콘 PNG → Android drawable 복사 (5개)
- [ ] `TabItem`에 실제 아이콘 리소스 연결
- [ ] NavigationBar 색상·폰트 스타일 적용
- [ ] 각 탭 Content를 실제 Screen으로 교체 (각 Screen 구현 후 순차적으로)

---

### 3단계: 탭 화면

#### 3-1. CurrentTemperatureScreen
**경로:** `presentation/currenttemperature/screen/CurrentTemperatureScreen.kt`
**iOS 참고:** `Presentation/CurrentTemperature/View/CurrentTemperatureView.swift`
- [ ] Figma 노드 확인 → iOS 구조 분석 → Screen 구현 → ViewModel 연결 → MainTab 연결

#### 3-2. SeaAnalysisScreen
**경로:** `presentation/seaanalysis/SeaAnalysisScreen.kt`
**iOS 참고:** `Presentation/SeaAnalysis/SeaAnalysisView.swift`
- [ ] Figma 노드 확인 → Screen 구현 → ViewModel 연결 → MainTab 연결

#### 3-3. FishingRecordScreen
**경로:** `presentation/fishingrecord/screen/FishingRecordScreen.kt`
**iOS 참고:** `Presentation/FishingRecord/View/FishingRecordView.swift`
**특이사항:** KakaoMap/GoogleMap AndroidView 래핑 필요
- [ ] `FishingRecordScreen.kt` + `KakaoMapView.kt` + `GoogleMapView.kt` 구현

#### 3-4. HistoryScreen
**경로:** `presentation/history/HistoryScreen.kt`
**iOS 참고:** `Presentation/History/View/HistoryView.swift`
- [ ] Figma 노드 확인 → Screen 구현 → ViewModel 연결 → MainTab 연결

#### 3-5. SettingScreen
**경로:** `presentation/setting/SettingScreen.kt`
**iOS 참고:** `Presentation/Setting/SettingView.swift`
- [ ] Figma 노드 확인 → Screen 구현 → ViewModel 연결 → MainTab 연결

---

### 4단계: 서브 화면

| 화면 | 경로 | iOS 참고 | 특이사항 |
|------|------|---------|---------|
| `OceanSelectScreen` | `presentation/currenttemperature/screen/` | `Presentation/CurrentTemperature/View/OceanSelectView.swift` | |
| `SeaAnalysisDetailScreen` | `presentation/seaanalysis/` | `Presentation/SeaAnalysis/SeaAnalysisDetailView.swift` | 온도 꺾은선 그래프 Canvas |
| `HistoryDetailScreen` | `presentation/history/` | `Presentation/History/View/HistoryDetailView.swift` | |
| `HistoryImageViewer` | `presentation/history/` | `Presentation/History/View/HistoryImageViewer.swift` | |

---

## ⚠️ 트러블슈팅 핵심 지침

### local.properties 필수 키
Screen 구현 전 아래 키가 모두 설정되어 있는지 확인:
```
RISA_API_KEY, API_BASE_URL, ONBADA_BASE_URL, GOOGLE_MAPS_KEY, KAKAO_APP_KEY
```

### async + launch 코루틴 예외 처리
`viewModelScope.launch` 내부에서 `async`를 사용할 때 반드시 `supervisorScope { }` 로 감싸야 한다.
```kotlin
viewModelScope.launch {
    supervisorScope {
        val deferred = async { networkCall() }
        try { deferred.await() } catch (e: Exception) { ... }
    }
}
```

### 이미지 리소스 관리 원칙
- **아이콘은 Vector Drawable(XML) 금지 — 반드시 PNG로 해상도별 폴더 관리**
- iOS @2x/@3x → Android 밀도별 폴더 변환 (24dp 아이콘 기준):
  ```
  @2x 원본(48px) → drawable-xhdpi/
  @3x 원본(72px) → drawable-xxhdpi/
  @2x 리사이즈   → drawable-mdpi/ (24px), drawable-hdpi/ (36px)
  @3x 리사이즈   → drawable-xxxhdpi/ (96px)
  ```
  ```bash
  sips -z 24 24 src@2x.png --out drawable-mdpi/ic_name.png
  sips -z 36 36 src@2x.png --out drawable-hdpi/ic_name.png
  cp src@2x.png drawable-xhdpi/ic_name.png
  cp src@3x.png drawable-xxhdpi/ic_name.png
  sips -z 96 96 src@3x.png --out drawable-xxxhdpi/ic_name.png
  ```
- `res/drawable`은 서브폴더 불가 → 네이밍 접두사로 그룹핑:
  - `ic_tab_` : 탭바 아이콘
  - `ic_`     : 일반 아이콘
  - `img_`    : 일러스트/이미지
  - `bg_`     : 배경
  - `splash_` : 스플래시 전용
- Figma `get_design_context` URL(`http://localhost:3845/assets/...`)은 `curl`로 다운로드 후 위 규칙 적용
- `R.mipmap.ic_launcher_*`는 adaptive icon(배경 포함)이므로 커스텀 화면에서 재사용 금지

### Android 12+ 시스템 Splash
- `windowBackground` 방식은 Android 12+에서 무효 → `androidx.core:core-splashscreen` 사용
- `installSplashScreen()`은 반드시 `super.onCreate()` **이전** 호출
- `windowSplashScreenBackground`는 solid color만 지원 (gradient 무효)
- 시스템 Splash 아이콘 권장: 288dp (실제 표시 192dp), 밀도별: mdpi=288px, hdpi=432px, xhdpi=576px, xxhdpi=864px, xxxhdpi=1152px

---

## 🍎 Figma → Android 변환 규칙

### 수치 변환
- Figma pt = Android dp (1:1, 변환 불필요)
- Figma 폰트 크기 pt = Android sp (1:1)
- 폰트: iOS SF Pro → Android **Noto Sans KR**

### 무시할 Figma 레이어
- `IosStatusBar`, `IosStatusBar2` — Android 시스템 처리
- `HomeIndicator` — `navigationBarsPadding()` 처리

### 각 화면 구현 워크플로우
```
1. Figma 노드 ID 확인 (get_metadata)
2. Figma 디자인 상세 조회 (get_design_context)
3. iOS 대응 View 파일 읽기
4. Android Composable 구현
5. ViewModel 연결 → Navigation 연결 → 빌드 확인
```

### iOS → Android 패턴 치트시트

| iOS (SwiftUI) | Android (Compose) |
|---------------|-------------------|
| `ZStack` | `Box` |
| `VStack` | `Column` |
| `HStack` | `Row` |
| `Image("name")` | `Image(painterResource(R.drawable.name))` |
| `.frame(width:height:)` | `.size(width, height)` |
| `.ignoresSafeArea()` | `fillMaxSize()` + WindowInsets 설정 |
| `@State` | `remember { mutableStateOf() }` |
| `@StateObject` | `remember { ViewModel() }` |
| `.task { }` / `.onAppear { }` | `LaunchedEffect(Unit) { }` |
| `onChange(of:) { }` | `LaunchedEffect(state) { }` |
| `.alert(isPresented:)` | `if (show) AlertDialog { }` |
| `NavigationLink { }` | `navController.navigate("route")` |
| `TabView { }` | `NavigationBar + Scaffold` |
| `@Published` | `MutableStateFlow` + `StateFlow` |
| `UIViewRepresentable` | `AndroidView { }` |
| `FullScreenCover` | `Dialog(usePlatformDefaultWidth = false)` |
| `Sheet` | `ModalBottomSheet` |
