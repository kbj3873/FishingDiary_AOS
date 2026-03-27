# SeaThermo Android UI 구현 작업 로그

> **목적:** Figma 디자인 + iOS(SwiftUI) 참고 코드를 기반으로 Android Jetpack Compose UI를 정밀하게 이식한다.

---

## 🚨 작업 행동 원칙 (필수 준수)

### 수정 전 반드시 확인해야 할 사항

1. **영향도 파악 먼저** — 수정 대상 리소스(이미지, 컴포넌트, 함수 등)가 다른 곳에서도 사용되고 있는지 반드시 확인한다. 확인 전에 수정하지 않는다.

2. **선택·판단·비즈니스 로직은 사용자에게 질문** — 아래 상황에서는 수정하지 않고 먼저 방안을 보고하고 사용자의 결정을 기다린다.
   - 여러 해결 방안이 존재할 때
   - 기존 동작 방식을 변경할 때
   - 이미지·리소스 파일을 교체/추가할 때
   - 애니메이션·전환 방식 등 UX에 영향을 주는 변경일 때

3. **보고 → 승인 → 수정 순서** — 분석 결과와 방안을 먼저 보고하고, 사용자가 "수정해" 또는 특정 방안을 선택한 후에만 코드/파일을 변경한다.

4. **추론으로 결정 금지** — 불확실한 부분은 추론하여 임의로 결정하지 말고 사용자에게 질문한다.

5. **확률적 추론 금지** — "아마 이럴 것이다", "이 값이면 될 것이다" 식의 추측으로 코드를 작성하지 않는다. 확실하지 않으면 반드시 근거(코드 확인, iOS 대응 코드, SDK 문서)를 먼저 확보한다.

6. **영향도가 있는 수정은 반드시 조언 구하기** — 수정이 다른 화면/컴포넌트/동작에 영향을 줄 수 있다고 판단되면 수정 전에 영향 범위를 명시하고 사용자의 확인을 받는다.
   - 전역 상태(FDAppManager, Registry 등) 변경 시
   - 공통 컴포넌트(KakaoRecordMapView 등 여러 화면에서 쓰는 것) 수정 시
   - 생명주기·렌더링 방식 변경 시 (pause/resume, DisposableEffect 등)
   - 아키텍처 패턴 변경 시 (새로운 전역 객체 도입 등)

### 올바른 작업 흐름

```
1. 요구사항 파악
2. 관련 코드/리소스 읽기 (영향도 포함)
3. 분석 결과 및 해결 방안 보고
   - 방안이 여럿이면 각각의 장단점과 영향 범위를 명시
   - 영향도가 있는 부분은 반드시 언급하고 조언 요청
4. 사용자 승인/선택 대기
5. 수정 실행
6. 결과 확인
```

### 질문해야 하는 상황 예시 (이전 세션 경험 기반)

| 상황 | 잘못된 행동 | 올바른 행동 |
|------|------------|------------|
| 마커 크기가 너무 작다고 함 | 임의로 3배 확대 | "2배/3배 중 어느 정도가 맞을까요?" 질문 |
| 두 가지 수정 방안 존재 | 더 복잡한 방안 임의 선택 | 두 방안 모두 보고 후 선택 요청 |
| SDK 동작이 불확실 | 추측으로 코드 작성 | iOS 코드 또는 SDK 문서 확인 후 근거 제시 |
| 전역 레지스트리 도입 | 바로 구현 | "전역 객체 도입이 필요한데 괜찮으신가요?" 확인 |
| 수정이 실패했을 때 | 다른 방법으로 계속 시도 | 실패 원인 분석 보고 후 방향 재결정 요청 |

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

## 📊 현재 구현 상태 (2026-03-27 기준)

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
| `MainTabScreen.kt` | 🟡 진행중 | 전체 탭 연결 완료. Lazy+Persistent 렌더링 적용 |
| `AppNavigation.kt` | 🟡 진행중 | splash/onboarding/main/webview/sea_analysis_detail/history_detail route 존재 |
| `CrawlingCurrentTemperatureScreen.kt` | ✅ 완성 | OceanSelect Sheet 연결 완료 |
| `CurrentTemperatureScreen.kt` | ✅ 완성 | OceanSelect Sheet 연결 완료 |
| `CrawlingOceanSelectScreen.kt` | ✅ 완성 | |
| `OceanSelectScreen.kt` | ✅ 완성 | |
| `SeaAnalysisScreen.kt` | ✅ 완성 | MainTab 연결, onNavigateToDetail 콜백 연결 완료 |
| `SeaRegionListScreen.kt` | ✅ 완성 | ModalBottomSheet + SeaRegionRowView 구현 완료 |
| `SeaAnalysisDetailScreen.kt` | ✅ 완성 | Canvas 꺾은선 그래프 + 수온 카드 3열 + Footer 구현 완료 |
| `FishingRecordScreen.kt` | 🟡 QA 중 | 카카오맵 마커 크기·anchor·현재위치 유지 버그 수정 완료. 실기기 QA 필요 |
| `HistoryScreen.kt` | 🟡 QA 중 | 진입/닫힘 애니메이션 None 적용. 실기기 QA 필요 |
| `HistoryDetailScreen.kt` | 🟡 QA 중 | 지도 렌더링·zoom-to-fit·레이어 잔류 버그 수정 완료. 실기기 QA 필요 |
| `HistoryImageViewer.kt` | ✅ 완성 | |

---

## ✅ 완료된 작업: HistoryScreen / HistoryDetailScreen / HistoryImageViewer (2026-03-26)

### 구현 완료 항목
- HistoryScreen: LazyColumn 기록 목록 + HistoryRecordCardView
- HistoryDetailScreen: GoogleHistoryMapView(경로/마커) + 상단 요약 카드 + 하단 마커 오버레이(StateMarkerCard / PhotoMarkerCard) + 조과물 사진 LazyRow
- HistoryImageViewer: HorizontalPager 풀스크린 뷰어 + 삭제 기능
- AppNavigation: history_detail route 추가
- MainTabScreen: History Placeholder 제거 및 HistoryScreen 연결

### 버그 수정 이력 (2026-03-26)
- `FDLocationManager.startTracking()`: `_currentMapLine` (0,0) 리셋 — 이전 세션 위치가 새 세션 첫 지점으로 저장되는 버그 수정
- `LocationTrackingService`: `setWaitForAccurateLocation(false → true)` — 실내/저신호 환경 GPS 튀는 현상 개선
- `FishingRecordScreen` 카메라: `TakePicturePreview` → `TakePicture(FileProvider URI)` 전환 — 풀 해상도 원본 저장
- `DefaultFishingRecordRepository.savePhoto()`: 원본(uuid.jpg) + 썸네일(uuid_thumb.jpg, 300px JPEG 70%) 분리 저장
- `loadPhotoMarkerBitmap` / `loadHistoryPhotoMarkerBitmap`: uuid_thumb.jpg 우선 로드, fallback uuid.jpg
- `GoogleHistoryMapView.zoomToFit()`: `mapView.post` → `setOnMapLoadedCallback` 전환 — 레이아웃 완료 전 호출 시 고정 줌(13f) 폴백 버그 수정, padding 300px
- `StateMarkerCard` 지점명 텍스트: `graphicsLayer { translationY = -3f }` 세로 정렬 보정

---

## ✅ 완료된 작업: Kakao Maps 전환 (2026-03-26)

### 구현 완료 항목
- `FDAppManager`: `mapTypeFlow: StateFlow<MapType>` 추가, `isRecording` 플래그 추가, 기본값 → KAKAO_MAP
- `SettingViewModel`: 낚시 기록 중 지도 변경 차단 (`isRecording` 체크 → `showRecordingBlockedDialog`)
- `SettingScreen`: 지도 변경 불가 AlertDialog 추가
- `FishingRecordViewModel`: `startRecording()` / `stopRecording()` 에서 `FDAppManager.setRecording()` 연동
- `KakaoRecordMapView.kt` 신규 생성 — AndroidView 래퍼, lifecycle 관리
- `FishingRecordScreen`: `mapTypeFlow.collectAsStateWithLifecycle()` 구독, `when(currentMapType)` 분기로 Google/Kakao 전환
- `KakaoHistoryMapView.kt` 신규 생성 — 폴리라인/상태마커/사진마커/마커 탭 콜백 구현
- `HistoryDetailScreen`: `mapTypeFlow` 구독, `when(currentMapType)` 분기로 Google/Kakao 전환

### Kakao Maps SDK 2.13.0 실제 API (JAR 검증)
SDK 문서와 다른 실제 클래스/메서드 — 트러블슈팅 시 참고:
| 잘못된 API (컴파일 오류) | 올바른 API |
|------------------------|-----------|
| `LabelTextureStyle` | 존재하지 않음 — `LabelStyle.from(Bitmap)` 직접 사용 |
| `MapPolylineShapeLayerOptions` | `ShapeLayerOptions` |
| `MapPolylineShape` | 없음 — `ShapeLayer.addPolyline(PolylineOptions.from(MapPoints, PolylineStyle))` |
| `addMapPolylineShapes(list)` | `ShapeLayer.addPolyline(PolylineOptions)` |
| `PolylineOptions.from(stylesSet)` | `PolylineOptions.from(MapPoints, PolylineStyle)` |
| `LabelOptions.setId(id)` | `LabelOptions.from(id, LatLng)` (첫 파라미터가 ID) |
| `LabelLayer.remove()` (no-arg) | `labelManager.remove(labelLayer)` |
| `ShapeLayer.remove()` (no-arg) | `shapeManager.remove(shapeLayer)` |
| `setOnPoiClickListener { result -> result?.itemId }` | `setOnLabelClickListener { _, _, label -> label.getTag() as? String; true }` |

### 설계 결정 사항
- 낚시 기록 중 지도 변경 차단: `FDAppManager.isRecording` 플래그 기반, `SettingViewModel`에서 체크
- 히스토리 상세 진입 후 설정 변경 시 자동 전환: `mapTypeFlow` 구독으로 리컴포지션 자동 처리
- 레이어 초기화: `shapeManager.remove(layer)` / `labelManager.remove(layer)` 패턴 사용
- 클릭 식별: `LabelOptions.from(id, LatLng)` + `.setTag(id)` 조합

---

## ✅ 완료된 작업: 카카오맵 실기기 버그 수정 (2026-03-26)

### Bug 1: FishingRecordScreen — 카카오맵 현재 위치 아이콘 미표시

**원인:** Kakao Maps SDK에는 Google Maps의 `isMyLocationEnabled`에 해당하는 내장 현재 위치 표시 기능이 없음.

**수정 내용 (`FishingRecordScreen.kt`)**
- `kakaoLocationLabelLayer` (zOrder: 99999) 신규 추가
- `LaunchedEffect(kakaoMap, locationPermissionGranted)`: 초기 카메라 이동 후 `btn_my_location` 비트맵으로 현재 위치 마커 표시
- Kakao `mapLineEvents.collect`: GPS 업데이트마다 `Label.moveTo(LatLng)` 로 마커 위치 갱신 (레이어 재생성 없이 성능 최적화)
- 기록 종료(`isRecording` false) 및 지도 타입 변경 시 위치 레이어도 함께 초기화
- `showKakaoLocationMarker()` 헬퍼 함수 추가

**JAR 검증으로 확인한 API:** `Label.moveTo(LatLng)` 존재 확인 ✓

---

### Bug 2: KakaoHistoryMapView — 경로·마커 미렌더링 및 카카오 본사 위치 고정

**원인 (설계 버그):** `kakaoMapRef = remember { arrayOfNulls<KakaoMap>(1) }` — Compose가 관찰할 수 없는 일반 배열 사용.
- `onMapReady`에서 배열 값을 설정해도 Compose는 변경을 감지하지 못함
- `LaunchedEffect`가 재실행되지 않아 영구적으로 빈 지도 표시
- `factory` 클로저에 캡처된 `polylines` 등이 초기 빈 리스트라 fallback 체크도 무효

**수정 내용 (`KakaoHistoryMapView.kt`)**
- `arrayOfNulls<KakaoMap>(1)` → `var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }` 교체
- `LaunchedEffect` key에 `kakaoMap` 추가: `LaunchedEffect(kakaoMap, polylines.size, stateMarkers.size, photoMarkers.size)`
- `onMapReady` 콜백 단순화: `kakaoMap = map` 설정만 수행 (factory 클로저 fallback 제거)
- `kakaoZoomToFit()` 호출을 `mapView.post { }` 로 감싸 렌더링 완료 후 카메라 이동 보장
- `import` 추가: `mutableStateOf`, `getValue`, `setValue`

---

### Bug 3: 히스토리 탭 → 낚시기록 탭 이동 시 히스토리 레이어 잔류

**원인:** Kakao Maps SDK는 전역 GL 렌더링 엔진을 공유. `MainTabScreen`의 Lazy+Persistent 렌더링으로 양 화면이 동시에 composition에 존재할 때 히스토리 레이어가 낚시기록 지도에 표시됨. `mapView.finish()` 만으로는 레이어가 제거되지 않음.

**수정 내용 (`KakaoHistoryMapView.kt`)**
- `DisposableEffect.onDispose`: `mapView.finish()` 전 히스토리 레이어 3개 명시적 제거
  ```
  sm.remove("history_poly_layer")
  lm.remove("history_state_layer")
  lm.remove("history_photo_layer")
  ```

---

### Bug 4: HistoryDetailScreen 뒤로가기 전환 어색함

**원인:** `history_detail` 라우트에 transition 미지정 → 기본 전환이 Compose UI와 AndroidView(MapView) 렌더링 타이밍을 따로 처리하여 "상단 카드 먼저 사라지고 지도가 뒤늦게 사라지는" 2단계 소멸 현상 발생.

**수정 내용**
- `HistoryScreen.kt` — `history_detail` composable에 전환 애니메이션 명시:
  - `enterTransition = fadeIn(tween(300))` — 진입 시 자연스럽게 나타남
  - `popExitTransition = fadeOut(tween(250))` — 뒤로가기 시 전체가 함께 페이드아웃
- `HistoryDetailScreen.kt` — 루트 `Box`에 `background(Color.Black)` 추가: fadeOut 중 AndroidView 투명홀 방지

---

## ✅ 완료: 카카오맵 지도 공유 버그 수정 (2026-03-27)

### Bug 5: 카카오맵 마커/썸네일 크기 불일치 (구글맵 대비 너무 큼)

**원인:** `LabelStyle.from(Bitmap)`은 비트맵 픽셀을 dp처럼 해석함.
- Google Maps: `48 * density` px 비트맵 → SDK 내부에서 density 나눔 → 48dp 표시 ✓
- Kakao Maps: `48 * density` px 비트맵 → dp로 그대로 해석 → `48 * density` dp 표시 ✗ (3배 이상 큼)

**수정 내용 (`KakaoHistoryMapView.kt`)**
- `loadHistoryPhotoMarkerBitmap`: `imageSize = (48 * density).toInt()` → `96` (density 제거 후 2배)
- `borderWidth`: `(3 * density).toInt()` → `6`
- `cornerRadius`: `10f * density` → `20f`
- 상태 마커: `Bitmap.createScaledBitmap(bitmap, bitmap.width/density*2, bitmap.height/density*2)`

**FishingRecordScreen에 동일 적용 (2026-03-27)**
- 상태 마커: `LaunchedEffect(uiState.markers.size)` 내 동일한 `createScaledBitmap(bitmap, bitmap.width/density*2, ...)` 적용
- 사진 마커: `loadPhotoMarkerBitmap`(Google용, density 곱함) → `loadKakaoPhotoMarkerBitmap`(Kakao 전용, 고정값 96/6/20f) 분리
  - Google Maps 사진 마커는 기존 `loadPhotoMarkerBitmap` 그대로 유지
  - Kakao Maps 사진 마커만 `loadKakaoPhotoMarkerBitmap` 호출로 변경

---

### Bug 6: 히스토리 상세 → 낚시기록 탭 이동 시 히스토리 레이어 잔류 (백스택 케이스)

**재현 경로:** 앱 → 낚시기록 → 히스토리 → 상세 → **낚시기록 탭** → 히스토리 탭(상세 유지) → 낚시기록
**기존 Bug 3 수정(onDispose 레이어 제거)으로 해결 안 되는 케이스:**
- `popBackStack()` 없이 탭 전환만 → `KakaoHistoryMapView`가 백스택에 생존 → `onDispose` 미호출

**수정 내용**
- `KakaoHistoryMapView`: `isVisible: Boolean` 파라미터 추가
- `LaunchedEffect(isVisible, kakaoMap)`: `isVisible=false` 시 레이어 3개 제거, `true` 시 재드로잉
- `HistoryDetailScreen`: `isVisible` 파라미터 추가 → `KakaoHistoryMapView`에 전달
- `HistoryScreen.kt` (`HistoryTabNavHost`): `HistoryDetailScreen`에 `isVisible` 전달

---

### Bug 7: 카카오맵 카메라 위치 공유 (히스토리 상세 줌 위치가 낚시기록 지도에 표시)

**원인:** Kakao Maps SDK는 전역 GL 엔진 1개 공유. 두 MapView가 동시에 `resume()` 상태이면
카메라 위치가 전역으로 공유됨. iOS는 `viewWillDisappear → stopRendering()`으로 해결하지만
Android Compose의 Lazy+Persistent 렌더링에서는 탭 전환 시 생명주기 콜백이 없음.

**시도했다가 실패한 접근:**
- `isVisible` prop drilling → `KakaoRecordMapView`의 `LaunchedEffect(isVisible)` → `mapView.pause()/resume()`
- 실패 원인: `LaunchedEffect` 자체는 실행되지 않음 (`FishingRecordScreen`은 recompose 되지만
  `KakaoRecordMapView` 안의 `LaunchedEffect`가 동작하지 않는 현상 → 근본 원인 미파악)
- `FishingRecordScreen`이 recompose된다고 해서 MapView가 다시 그려지는 건 아님
  (`remember { MapView }` 로 인스턴스 유지, `LaunchedEffect`는 key가 바뀔 때만 재실행)

**현재 적용된 수정 (빌드 오류 수정 완료, 실기기 검증 필요):**

`KakaoMapViewRegistry` 패턴 — MainTabScreen이 탭 전환을 직접 감지하여 MapView 제어

```
신규 파일: managers/KakaoMapViewRegistry.kt
  - recordMapView: MapView?   ← KakaoRecordMapView가 등록/해제
  - historyMapView: MapView?  ← KakaoHistoryMapView가 등록/해제
```

수정 파일:
- `KakaoRecordMapView.kt`: `DisposableEffect(Unit)` 추가 → 진입 시 registry 등록, 이탈 시 null
- `KakaoHistoryMapView.kt`: `DisposableEffect(Unit)` 추가 → 진입 시 registry 등록, 이탈 시 null
- `MainTabScreen.kt`: `LaunchedEffect(selectedTabIndex)` 추가
  ```kotlin
  when (selectedTabIndex) {
      2 -> { recordMapView?.resume(); historyMapView?.pause() }  // 낚시기록
      3 -> { historyMapView?.resume(); recordMapView?.pause() }  // 히스토리
      else -> { recordMapView?.pause(); historyMapView?.pause() }
  }
  ```

**최종 적용 방식 (KakaoMapViewRegistry 삭제 + 강제 닫기):**
- `KakaoMapViewRegistry.kt` 삭제 — 전역 레지스트리 불필요 확인
- `HistoryTabNavHost`: `LaunchedEffect(isVisible=false)` → `history_detail` 화면 강제 `popBackStack()` → `KakaoHistoryMapView.onDispose` 자동 호출 → GL 레이어/리소스 정리
- `KakaoRecordMapView`: `isVisible` 파라미터 추가 → `LaunchedEffect(isVisible)` + `LifecycleObserver(isVisible 체크)` 로 pause/resume 처리
- `KakaoHistoryMapView`: `isVisible` 파라미터 및 관련 `LaunchedEffect` 제거 (강제 닫기로 대체)
- `FishingRecordScreen` → `KakaoRecordMapView`로 `isVisible` prop drilling
- `MainTabScreen` → `FishingRecordScreen`으로 `isVisible` 전달

**해결된 버그:**
- Bug 5 (마커 크기): KakaoHistoryMapView + FishingRecordScreen 모두 수정 완료
- Bug 6 (히스토리 레이어 잔류): `onDispose` 레이어 클리어로 해결
- Bug 7 (카메라 공유): 강제 닫기 + `isVisible` 기반 pause/resume으로 해결

---

## ✅ 완료: FishingRecordScreen 카카오맵 추가 버그 수정 (2026-03-27)

### Bug 8: 현재위치 아이콘 Y offset — 실제 GPS 포인트보다 위로 올라가 보임

**원인:** `LabelStyle.from(bitmap)` 기본 앵커가 이미지 하단 중앙 `(0.5f, 1.0f)` → 원형 아이콘이 GPS 좌표 위에 떠 있는 것처럼 표시

**수정 내용 (`FishingRecordScreen.kt` — `showKakaoLocationMarker`)**
- `LabelStyle.from(bitmap)` → `LabelStyle.from(bitmap).setAnchorPoint(0.5f, 0.5f)`
- JAR 검증으로 `setAnchorPoint(float, float)` API 존재 확인 ✓

---

### Bug 9: 기록 중단 후 현재위치 마커 사라짐 + 내 위치 버튼 무반응

**원인 1 (마커 사라짐):** `LaunchedEffect(uiState.isRecording)` 에서 기록 중단 시 `kakaoLocationLabelLayer`를 제거·재생성하고 `kakaoLocationLabel = null` 초기화. 이후 `mapLineEvents`는 `isRecording=false`로 `return@collect` → 마커가 영구적으로 미표시

**원인 2 (버튼 무반응):** `onMyLocation` 핸들러 Kakao 분기에 카메라 이동만 있고 마커 표시/이동 로직 없음

**수정 내용 (`FishingRecordScreen.kt`)**
- 기록 중단 시: `kakaoLocationLabelLayer`와 `kakaoLocationLabel`을 초기화하지 않고 유지
  - 경로/마커 레이어(`kakaoPolyLayer`, `kakaoStateLabelLayer`, `kakaoPhotoLabelLayer`)만 초기화
  - 위치 레이어는 지도 타입 변경 시(`LaunchedEffect(currentMapType)`)와 `onDispose`에서만 정리
- 내 위치 버튼: Kakao 분기 `lastLocation` / `getCurrentLocation` 두 케이스 모두 `moveTo()` 또는 `showKakaoLocationMarker()` 추가

---

### Bug 10: 히스토리 상세 진입/닫힘 시 스와이프 애니메이션 부자연스러움

**원인:** `enterTransition = slideInHorizontally`, `popExitTransition = slideOutHorizontally` 설정 → 탭 전환 시 `popBackStack()` 호출에도 슬라이드 모션 발생. AndroidView(KakaoMapView)는 Compose 애니메이션에 참여하지 않아 더욱 부자연스러움

**수정 내용 (`HistoryScreen.kt`)**
- `enterTransition = { EnterTransition.None }`
- `popExitTransition = { ExitTransition.None }`
- 미사용 `slideInHorizontally`, `slideOutHorizontally`, `tween` import 제거

---

## 🔨 다음 작업: 실기기 QA

### QA 체크리스트
```
FishingRecordScreen (카카오맵) — 오늘 수정 항목 우선 검증
  □ [Bug 5] 상태 마커 크기 구글맵과 동일한지 확인
  □ [Bug 5] 사진 마커(썸네일) 크기 구글맵과 동일한지 확인
  □ [Bug 8] 현재위치 아이콘 경로라인 정중앙에 위치하는지 확인
  □ [Bug 9] 기록 중단 후 현재위치 마커 유지 확인
  □ [Bug 9] 기록 중단 후 내 위치 버튼 클릭 시 마커 갱신 확인
  □ 앱 최초 진입 시 현재 위치 마커 표시 확인
  □ 기록 시작 후 이동 시 위치 마커 실시간 갱신 확인
  □ 기록 시작 → 경로선 실시간 업데이트
  □ 상태 변경 시 상태 마커 추가
  □ 사진 촬영 시 사진 마커 추가
  □ 설정에서 구글맵 전환 → FishingRecordScreen 지도 교체
  □ 낚시 기록 중 설정 진입 → 지도 변경 차단 팝업 노출
  □ [Bug 7] 히스토리 상세 확대 후 낚시기록 탭 이동 시 카메라 위치 정상

HistoryDetailScreen (카카오맵)
  □ [Bug 10] 진입 시 애니메이션 없이 즉시 표시 확인
  □ [Bug 10] 닫힘 시 애니메이션 없이 즉시 닫힘 확인
  □ 히스토리 상세 진입 시 전체 경로/마커/썸네일 렌더링 확인
  □ 카메라 자동 zoom-to-fit 확인
  □ 상태 마커 탭 → StateMarkerCard 하이라이트
  □ 사진 마커 탭 → PhotoMarkerCard 하이라이트
  □ [Bug 6] 히스토리 상세 백스택 유지 후 낚시기록 탭 이동 시 레이어 잔류 없음
  □ 설정에서 지도 변경 후 복귀 → 자동 전환

공통
  □ 구글맵 ↔ 카카오맵 전환 후 이전 지도 리소스 정상 해제
  □ 앱 백그라운드 진입/복귀 시 지도 resume/pause 정상 동작
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
