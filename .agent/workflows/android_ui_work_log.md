# SeaThermo Android UI 구현 작업 로그

> **목적:** Figma 디자인 + iOS(SwiftUI) 참고 코드를 기반으로 Android Jetpack Compose UI를 정밀하게 이식한다.

---

## 📌 핵심 참고 자료

| 자료 | 경로/위치 | 용도 |
|------|----------|------|
| iOS 소스 | `/Users/kimbyeongjoon/Documents/workspace_seathermo/SeaThermo_iOS/SeaThermo/Presentation/` | UI 구조, 로직 참고 |
| Figma | MCP 연결 (현재 선택 노드 기준 조회) | 정확한 크기·색상·간격 |
| Android ViewModel | `SeaThermo_AOS/app/src/main/.../presentation/` | 이미 완성된 비즈니스 로직 |
| Android AppNavigation | `SeaThermo_AOS/app/.../presentation/AppNavigation.kt` | NavHost 진입점 |

---

## ⚡ Crawling / OpenAPI 쌍(Pair) 구현 원칙

`CurrentTemperature` 관련 화면은 **동일한 UI를 공유하는 두 세트**로 구성됩니다.

| Crawling 버전 | OpenAPI 버전 | 차이점 |
|--------------|-------------|--------|
| `CrawlingCurrentTemperatureScreen.kt` | `CurrentTemperatureScreen.kt` | 데이터 소스만 다름 |
| `CrawlingOceanSelectScreen.kt` | `OceanSelectScreen.kt` | 데이터 소스만 다름 |

**규칙:** UI 변경 사항은 반드시 **두 세트에 동일하게 적용**해야 합니다.
현재는 **크롤링 버전 기준으로 구현** 중입니다.

---

## 🗺️ 전체 구현 순서

```
1단계 (Foundation)   → 테마 · 디자인 토큰
2단계 (Entry Flow)   → Splash → Onboarding → MainTab 연결
3단계 (Tab Screens)  → CurrentTemperature → SeaAnalysis → FishingRecord → History → Setting
4단계 (Sub Screens)  → SeaRegionList · SeaAnalysisDetail · HistoryDetail · HistoryImageViewer
5단계 (Polish)       → 공통 컴포넌트 정리 · 애니메이션 · QA
```

---

## 📊 현재 구현 상태 (2026-03-24 기준, 업데이트: FishingRecordScreen 구현 시작)

### ViewModel (비즈니스 로직) — 전체 완성 ✅
| 파일 | 상태 |
|------|------|
| `SplashViewModel.kt` | ✅ 완성 |
| `OnboardingViewModel.kt` | ✅ 완성 |
| `CurrentTemperatureViewModel.kt` | ✅ 완성 |
| `CrawlingCurrentTemperatureViewModel.kt` | ✅ 완성 |
| `OceanSelectViewModel.kt` | ✅ 완성 |
| `CrawlingOceanSelectViewModel.kt` | ✅ 완성 |
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
| `MainTabScreen.kt` | 🟡 진행중 | CurrentTemperature·SeaAnalysis·Setting 연결됨, FishingRecord·History Placeholder |
| `AppNavigation.kt` | 🟡 진행중 | splash/onboarding/main/webview/sea_analysis_detail 5개 route 존재 |
| `CrawlingCurrentTemperatureScreen.kt` | ✅ 완성 | OceanSelect Sheet 연결 완료 |
| `CurrentTemperatureScreen.kt` | ✅ 완성 | OceanSelect Sheet 연결 완료 |
| `CrawlingOceanSelectScreen.kt` | ✅ 완성 | |
| `OceanSelectScreen.kt` | ✅ 완성 | |
| `SeaAnalysisScreen.kt` | ✅ 완성 | MainTab 연결, onNavigateToDetail 콜백 연결 완료 |
| `SeaRegionListScreen.kt` | ✅ 완성 | ModalBottomSheet + SeaRegionRowView 구현 완료 |
| `SeaAnalysisDetailScreen.kt` | ✅ 완성 | Canvas 꺾은선 그래프 + 수온 카드 3열 + Footer 구현 완료 |
| `FishingRecordScreen.kt` | 🔴 미구현 | ← **현재 작업** |
| `HistoryScreen.kt` | 🔴 미구현 | |
| `HistoryDetailScreen.kt` | 🔴 미구현 | |
| `HistoryImageViewer.kt` | 🔴 미구현 | |

---

## 🔨 현재 작업: FishingRecordScreen

### iOS 참고 파일
- `Presentation/FishingRecord/View/FishingRecordView.swift` — 메인 화면
- `Presentation/FishingRecord/View/Components/RecordMapView.swift` — Apple Map (MKMapView) 래핑
- `Presentation/FishingRecord/View/Components/RecordKakaoMapView.swift` — Kakao Map 래핑

### 화면 구조 (iOS 기준)
```
Box (ZStack)
  ├── 지도 레이어 (mapType에 따라 KakaoMap 또는 GoogleMap)
  └── 오버레이 레이어 (Box)
        ├── TopInfoBar (상단 좌측)
        │     상태Dot + 상태텍스트 + [속도 + 단위토글(knots/km/h)] + 저장지점수
        ├── MapControlButtons (우측 중단)
        │     줌인 / 줌아웃 / 내위치
        └── BottomControlBar (하단 중앙)
              [안내텍스트 - 녹화 전만] + 녹화버튼(중앙) + 카메라버튼(우측)
팝업 (Overlay)
  ├── 기록 중단 확인 팝업
  ├── 위치 권한 요청 팝업
  └── 카메라 권한 요청 팝업
```

### 필요한 이미지 에셋
iOS 에셋(`SeaThermo_iOS/.../FishingRecord/`) → Android 밀도별 변환 필요:
- `btn_zoom_in` / `btn_zoom_out` / `btn_my_location` — 지도 컨트롤
- `btn_record_play` / `btn_record_stop` — 녹화 버튼
- `btn_camera` — 카메라 버튼
- `ic_map_marker_blue` / `ic_map_marker_orange` / `ic_map_marker_red` — 지도 상태 마커

### 구현 세부 단계 (순서대로 진행)
```
Step 1: 이미지 에셋 변환 (iOS @2x/@3x → Android 밀도별 폴더)
Step 2: FishingRecordScreen 기본 레이아웃 (지도 placeholder + 오버레이 UI)
Step 3: TopInfoBar 구현 (상태 표시, 속도, 단위 토글)
Step 4: MapControlButtons + BottomControlBar 구현
Step 5: 권한 처리 (위치 권한 런타임 요청)
Step 6: 카메라 연동 (사진 촬영 / ActivityResultLauncher)
Step 7: 지도 AndroidView 래핑 (GoogleMapView 또는 KakaoMapView)
Step 8: 지도에 경로선/마커 그리기 (polyline + annotation)
Step 9: 팝업 구현 (기록 중단, 권한 안내)
Step 10: MainTabScreen 연결 + 빌드 확인
```

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
