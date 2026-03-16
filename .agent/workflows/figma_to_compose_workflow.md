---
description: Figma 디자인을 분석하여 Jetpack Compose 코드로 변환하고, 에셋을 추출하며 템플릿 기반으로 기능을 구현하는 통합 워크플로우
---

# Feature Development & Figma to Jetpack Compose Workflow

이 워크플로우는 Figma 디자인을 의도에 맞게 Jetpack Compose 코드로 변환하고, 필요한 벡터 아이콘을 프로젝트에 추가하며, 템플릿을 통해 기능 구현을 시작하는 통합 파이프라인입니다.

## 1. 사전 분석 (Preparation)

작업을 시작하기 전 아래 사항들을 먼저 확인합니다.

1. **디자인 분석**: 구현할 디자인의 주요 화면 구성 요소(리스트, 버튼, 탭 등)와 로직을 파악합니다.
2. **구조 확인**: `CLAUDE.md`를 읽고, 재사용 가능한 기존 컴포넌트나 연관된 도메인 엔티티가 있는지 확인합니다.
3. **레거시 확인**: `CLAUDE.md`의 레거시 정책 섹션을 확인하여 건드리지 말아야 할 구 파일들을 파악합니다.

## 2. Compose 템플릿 기반 코드 생성 (Implementation)

새로운 화면(Screen)이나 기능을 만들 때 아래 템플릿을 사용하여 기본 뼈대를 잡습니다.

> **참고:** Android는 Gradle이 `src/main/java` 하위 파일을 자동 인식하므로, iOS(Xcode project.pbxproj)처럼 수동 등록이 필요 없습니다.

### Screen Composable 템플릿
```kotlin
@Composable
fun {ScreenName}Screen(
    viewModel: {ScreenName}ViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onAppear()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("{Title}") })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Content
        }
    }
}
```

### ViewModel 템플릿
```kotlin
class {ScreenName}ViewModel(
    private val useCase: {Feature}UseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow({ScreenName}UiState())
    val uiState: StateFlow<{ScreenName}UiState> = _uiState.asStateFlow()

    fun onAppear() {
        viewModelScope.launch {
            // 데이터 로드 로직
        }
    }
}

data class {ScreenName}UiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
```

## 3. Compose 변환 규칙 (UI/Layout/Style)

Figma 디자인을 디테일하게 코드로 옮길 때 다음의 변환 규칙을 따릅니다.

### 무시해야 할 시스템 UI 및 Safe Area
- **Android 시스템 UI 무시**: 상태바(Status Bar), 내비게이션 바(홈 인디케이터, 제스처바) 영역은 Android가 자동으로 처리하므로 직접 구현하지 않습니다.
- **WindowInsets 처리**:
  - 배경 색상/이미지가 상태바 영역까지 덮여야 할 경우 `Modifier.fillMaxSize()` + `enableEdgeToEdge()` 적용.
  - 콘텐츠는 기본적으로 `Scaffold`의 `paddingValues`를 준수하여 시스템 UI와 겹치지 않도록 합니다.

### 레이아웃 및 컴포저블 계층 구조

| Figma | Compose |
|---|---|
| `Frame (vertical)` | `Column` |
| `Frame (horizontal)` | `Row` |
| `Z-Index 중첩` | `Box` |
| 절대 좌표 (X/Y) | `Box` + `Modifier.align()` 또는 `offset()` 지양 |
| 제약조건(Constraints) | `ConstraintLayout` 또는 Alignment |
| Scroll (vertical) | `LazyColumn` (리스트) 또는 `Column` + `verticalScroll()` |
| Scroll (horizontal) | `LazyRow` 또는 `Row` + `horizontalScroll()` |

### 색상, 폰트 및 기타 규칙
- **색상**: Figma의 그라디언트는 `Brush.linearGradient()`로, 단일 색상은 `Color(0xFF4FACFE)` 형태로 변환합니다. (예: SeaThermo Primary Blue `#4FACFE`)
- **폰트**: 시스템 폰트는 `TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)`, 커스텀 폰트는 `FontFamily`를 정의하여 사용합니다.
- **언어 및 소통**: 모든 작업 관련 문서, 사용자 소통은 모두 **한국어**로 작성하는 것을 원칙으로 합니다.

## 4. Asset 추출 및 추가 (Vector Drawable)

Android는 벡터 드로어블(XML)을 직접 사용하므로, PNG 변환 작업이 불필요합니다.

### Step A: Figma에서 Asset (SVG) 수집 및 검증
1. Figma에서 필요한 아이콘 노드(Node)를 선택해 SVG 코드를 추출합니다.
2. SVG 코드가 배경이 투명(`fill="none"`)하고 의도한 형태를 띄는지 검증합니다.

### Step B: SVG → Vector Drawable 변환
Android Studio의 **Vector Asset Studio**를 사용하거나, 아래 구조로 직접 XML을 작성합니다.

```xml
<!-- res/drawable/ic_new_icon.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF000000"
        android:pathData="..." />
</vector>
```

### Step C: res/drawable 폴더 구성 규칙
- 파일명은 스네이크 케이스(`snake_case`) 필수 사용 (예: `ic_new_icon.xml`)
- 아이콘은 `ic_` 접두사, 배경/일러스트는 `bg_` 접두사를 사용합니다.
- 단색 템플릿 아이콘의 경우 XML에서 `android:tint` 속성으로 색상을 동적으로 제어할 수 있습니다.

### Step D: Compose에서 사용
```kotlin
// 벡터 드로어블 사용
Icon(
    painter = painterResource(id = R.drawable.ic_new_icon),
    contentDescription = "아이콘 설명",
    tint = MaterialTheme.colorScheme.primary
)

// 이미지 사용
Image(
    painter = painterResource(id = R.drawable.bg_ocean),
    contentDescription = null
)
```

### Step E: 최종 검증
- `res/drawable/` 폴더에 XML 파일이 정상 추가되었는지 확인합니다.
- Compose Preview에서 아이콘이 의도한 형태로 렌더링되는지 확인합니다.
- 기존 레거시 Activity/XML 코드와 불필요한 의존성 없이 독립적으로 화면이 동작하는지 테스트합니다.
