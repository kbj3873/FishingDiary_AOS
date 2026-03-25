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

## 📊 현재 구현 상태 (2026-03-25 기준)

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
| `MainTabScreen.kt` | 🟡 진행중 | 전체 탭 연결 완료. Lazy+Persistent 렌더링 적용. History만 Placeholder |
| `AppNavigation.kt` | 🟡 진행중 | splash/onboarding/main/webview/sea_analysis_detail 5개 route 존재 |
| `CrawlingCurrentTemperatureScreen.kt` | ✅ 완성 | OceanSelect Sheet 연결 완료 |
| `CurrentTemperatureScreen.kt` | ✅ 완성 | OceanSelect Sheet 연결 완료 |
| `CrawlingOceanSelectScreen.kt` | ✅ 완성 | |
| `OceanSelectScreen.kt` | ✅ 완성 | |
| `SeaAnalysisScreen.kt` | ✅ 완성 | MainTab 연결, onNavigateToDetail 콜백 연결 완료 |
| `SeaRegionListScreen.kt` | ✅ 완성 | ModalBottomSheet + SeaRegionRowView 구현 완료 |
| `SeaAnalysisDetailScreen.kt` | ✅ 완성 | Canvas 꺾은선 그래프 + 수온 카드 3열 + Footer 구현 완료 |
| `FishingRecordScreen.kt` | ✅ 완성 | GoogleMap 기반. 경로선/마커/사진마커/팝업 전체 구현 완료 |
| `HistoryScreen.kt` | 🔴 미구현 | ← **다음 작업** |
| `HistoryDetailScreen.kt` | 🔴 미구현 | |
| `HistoryImageViewer.kt` | 🔴 미구현 | |

---

## ✅ 완료된 작업: FishingRecordScreen (2026-03-25)

### 구현 완료 항목
- GoogleRecordMapView (AndroidView 래핑) + 경로선 / 상태마커 / 사진마커 지도 드로잉
- TopInfoBar: 상태 Dot + 상태 텍스트 + 속도 + knots/km/h 단위 토글 + 저장 지점 수
- MapControlButtons: 줌인 / 줌아웃 / 내 위치
- BottomControlBar: 안내 텍스트(대기 중) + 녹화 버튼(중앙) + 카메라 버튼(우측)
- 권한 처리: 위치(1차 거부 Rationale / 영구 거부 설정 안내), 카메라 동일 패턴
- 팝업: 기록 중단 확인 / 위치 권한 / 카메라 권한

### 버그 수정 이력
- `loadPhotoMarkerBitmap`: px 고정 → dp→px 변환 (density 반영), DST_IN 클리핑 버그를 `canvas.clipPath()`로 교체, Aspect Fill 추가
- `LocationTrackingService`: `setWaitForAccurateLocation(false)`, `firstOrNull` → `lastOrNull`, `MainLooper` → `HandlerThread` 분리
- 경로선 드로잉: `LaunchedEffect(uiState.currentMapLine)` → `LaunchedEffect(viewModel)` + `mapLineEvents SharedFlow` 직접 구독 (백그라운드 복귀 시 경로 점프 선 해결)

---

## 🔨 다음 작업: HistoryScreen

### iOS 참고 파일
- `Presentation/History/View/HistoryView.swift` — 조과 기록 목록
- `Presentation/History/View/HistoryRecordCardView.swift` — 목록 카드 컴포넌트
- `Presentation/History/View/HistoryImageViewer.swift` — 풀스크린 사진 뷰어
- `Presentation/History/View/Components/HistoryKakaoMapViewController.swift` — 상세 지도

### 구현 세부 단계
```
Step 1: HistoryScreen — 기록 목록 (LazyColumn + HistoryRecordCardView)
Step 2: HistoryDetailScreen — 상세 정보 (지도 + 경로 재생 + 통계)
Step 3: HistoryImageViewer — 풀스크린 사진 뷰어
Step 4: AppNavigation에 history_detail route 추가
Step 5: MainTabScreen History Placeholder 제거 및 HistoryScreen 연결
```

---

## ⚠️ 트러블슈팅 핵심 지침

### local.properties 필수 키
Screen 구현 전 아래 키가 모두 설정되어 있는지 확인:
```
RISA_API_KEY, API_BASE_URL, ONBADA_BASE_URL, GOOGLE_MAPS_KEY, KAKAO_APP_KEY
```

### Row 내 가로 정렬 컴포넌트 세로 중앙 맞추기

Row 안에 폰트 크기가 다른 Text나 Icon이 나란히 있을 때 세로 중앙이 어긋나 보이는 경우 아래 방법을 조합해서 사용한다.

#### Text + Text (폰트 크기 다를 때)
```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(modifier = Modifier.alignByBaseline(), text = "2026.03.25", fontSize = 16.sp, ...)
    Text(modifier = Modifier.alignByBaseline(), text = "•", fontSize = 16.sp, ...)
    // 작은 텍스트가 시각적으로 아래로 처지면 graphicsLayer로 픽셀 단위 보정
    Text(
        modifier = Modifier.alignByBaseline().graphicsLayer { translationY = -2f },
        text = "15:42 출발", fontSize = 14.sp, ...
    )
}
```

#### Icon + Text
```kotlin
Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(modifier = Modifier.size(14.dp).graphicsLayer { translationY = 1f }, ...)
    Text(text = label, ...)
}
```

#### 핵심 원칙
| 방법 | Row 크기 영향 | 용도 |
|------|-------------|------|
| `alignByBaseline()` | 없음 | Text+Text 기본 정렬 |
| `graphicsLayer { translationY = Xf }` | **없음** | 픽셀 단위 미세 보정 (드로우 단계만) |
| `padding(top/bottom)` | **있음** (Row 확장됨) | ❌ 정렬 보정 용도로 사용 금지 |
| `offset(y = X.dp)` | 없음이나 | dp 단위라 미세 조정 어려움 |

> `graphicsLayer { translationY }`는 픽셀 단위(px)이고 레이아웃 측정에 전혀 영향을 주지 않으므로, Row/Column 크기를 유지하면서 픽셀 단위 보정이 필요할 때 사용한다.

---

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
