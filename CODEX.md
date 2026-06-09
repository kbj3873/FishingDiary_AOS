# CODEX Android 작업 가이드

이 문서는 Codex가 SeaThermo Android 저장소에서 작업을 이어가기 위한 현행 가이드다. 작업 전에는 요청과 관련된 실제 소스 파일을 먼저 읽고, 문서와 코드가 다르면 코드를 우선한다.

## 기본 원칙

- 사용자와의 소통, 작업 요약, 문서 작성은 한국어로 한다.
- 관련 파일을 먼저 읽고 기존 Android 구조와 스타일에 맞춰 최소 범위로 수정한다.
- 사용자가 명시하지 않은 리팩토링, 포맷 변경, 파일 이동은 피한다.
- 기존 변경사항을 임의로 되돌리지 않는다.
- 빌드/테스트가 필요한 변경은 가능한 범위에서 직접 검증하고, 실행하지 못한 경우 이유를 남긴다.
- 검색은 우선 `rg`를 사용한다.

## 배포와 빌드 분기

현재 프로젝트는 Gradle flavor로 공개/내부 빌드를 분리한다.

공개 빌드:

```text
flavor: public
applicationId: com.onbada.seathermo
BuildConfig.INTERNAL_BUILD: false
```

내부 빌드:

```text
flavor: internal
applicationId: com.onbada.seathermo.internal
BuildConfig.INTERNAL_BUILD: true
```

Android Studio에서 선택할 수 있는 주요 variant:

```text
publicDebug
internalDebug
publicRelease
internalRelease
```

주의:

- 공개 앱 package name은 `com.onbada.seathermo`를 유지한다.
- 새 공개 앱을 만들거나 package name을 바꾸지 않는다.
- 공개 배포용은 기존 앱에 더 높은 `versionCode`로 올리는 방향을 유지한다.
- 현재 `versionCode`는 `5`다.

## 설정 파일과 키

Firebase:

- public: `app/src/public/google-services.json`
- internal: `app/src/internal/google-services.json`
- 기존 `app/google-services.json`은 public flavor 위치로 이동됐다.

Kakao:

- `local.properties`에서 앱 키를 읽는다.
- `KAKAO_APP_KEY_PUBLIC`, `KAKAO_APP_KEY_INTERNAL`을 우선 사용한다.
- 값이 없으면 `KAKAO_APP_KEY`로 fallback한다.
- manifest placeholder와 `BuildConfig.KAKAO_APP_KEY` 모두 flavor별로 주입된다.

기타 주요 local property:

```text
RISA_API_KEY
API_BASE_URL
ONBADA_BASE_URL
GOOGLE_MAPS_KEY
KAKAO_APP_KEY
KAKAO_APP_KEY_PUBLIC
KAKAO_APP_KEY_INTERNAL
```

## 공개/내부 기능 분리

### 공개 빌드

- 현재수온은 공식 OpenAPI 기반 화면을 사용한다.
- 수온분석 탭은 준비중 화면을 표시한다.
- 공개 빌드는 `/api/regions`를 호출하지 않는다.
- 국립수산과학원 서버 직접 호출 또는 온바다 내부 crawling용 직접 호출 UI를 노출하지 않는다.

### 내부 빌드

- 현재수온은 기존 crawling/직접 서버 호출 화면을 사용한다.
- 수온분석은 내부 분석 화면을 사용한다.
- 앱 시작 시 `/api/regions`를 호출해 관측소 목록을 캐시한다.

관련 진입점:

- `MainTabScreen.kt`: 현재수온/수온분석 탭 flavor 분기
- `SplashViewModel.kt`: internal 빌드에서만 `/api/regions` 호출
- `PublicSeaAnalysisPreparingScreen.kt`: public 수온분석 준비중 화면

## 수온분석 상세 그래프

내부 수온분석 상세 화면은 최근 7일 그래프 기준 선택을 제공한다.

선택 항목:

```text
날짜별
12시간
6시간
3시간
```

동작:

- 기본값은 `날짜별`이다.
- Android 표준 `DropdownMenu`를 사용한다.
- 선택 기준에 따라 subtitle을 변경한다.
- 7일 데이터의 30분 단위 샘플은 그대로 사용한다.
- 기준 변경은 x축 tick과 라벨 간격만 바꾼다.
- 날짜별은 가로 스크롤하지 않는다.
- 12/6/3시간은 plot 영역만 가로 스크롤한다.
- 시간 기준을 바꾸면 스크롤은 왼쪽 시작점으로 초기화된다.
- y축 온도 라벨은 왼쪽에 고정한다.

x축 라벨:

- 날짜별: 날짜만 표시한다. 예: `6/2`
- 시간별: `00시` tick에서만 날짜를 함께 표시한다. 예: `6/8\n00시`
- 나머지 시간별 tick은 시간만 표시한다. 예: `03시`, `06시`, `12시`

## 낚시기록

### 안내 팝업

- 낚시기록 탭 진입 시 안내 팝업을 표시한다.
- "다시 보지 않기"를 제공한다.
- 저장 키는 `hideFishingRecordGuidePopup`이다.

문구 취지:

```text
낚시 기록은 워킹 루어 낚시나 보트 낚시처럼 이동하며 포인트를 탐색하는 상황에 최적화되어 있습니다.
기록 중 이동 경로와 상태 변화를 저장하고, 사진으로 조과를 남겨 히스토리에서 다시 확인할 수 있습니다.
```

### 시작/종료 마커

리소스:

```text
ic_map_marker_start
ic_map_marker_end
```

현행 동작:

- 기록 시작 후 유효 좌표가 확보되면 시작 마커를 표시한다.
- 낚시기록 화면에서는 시작 마커만 표시한다.
- 종료 마커는 낚시기록 화면에서 노출하지 않는다.
- 기록 종료 후 경로와 임시 마커 상태를 초기화한다.
- 히스토리 상세에서는 시작/종료 마커를 모두 표시한다.

anchor:

```text
start/end marker: 0.5, 0.5
fishing/drifting pin marker: 0.5, 1.0
```

### 상태 전환 마커

상태 마커는 아래 전환에서만 생성한다.

```text
이동중 -> 탐색중
이동중 -> 낚시중
```

생성하지 않는 경우:

- 시작 직후 첫 상태가 탐색중/낚시중인 경우
- 탐색중/낚시중 사이 전환
- 탐색/낚시에서 이동중으로 바뀌는 경우

히스토리 상세에서도 같은 규칙을 유지한다.

## 히스토리

### 시작/종료 마커

- 시작 마커는 세션 첫 record에서 파생한다.
- 종료 마커는 세션 마지막 record에서 파생한다.
- Google/Kakao 지도 모두 endpoint marker에 클릭 id/tag를 연결한다.
- 시작/종료 마커 탭 시 기존 낚시/탐색 마커와 같은 하단 UI를 표시한다.
- 시작 제목: `낚시 시작`
- 종료 제목: `낚시종료`
- 사진 썸네일은 없다.

### 지점 수 계산

목록과 상세의 지점 수 계산 기준은 같아야 한다.

```text
시작 마커 1
+ 종료 마커 1, 단 record가 2개 이상일 때
+ 이동중 -> 탐색/낚시 상태 마커 개수
+ 사진 마커 개수
```

첫 시작 record는 상태 전환 카운트에서 제외한다.

### 상단 UI와 이미지 뷰어

- 상단 정보 카드는 accordion 방식으로 확장/축소한다.
- 뒤로 버튼, 날짜/시간, 삭제 버튼 영역은 고정한다.
- 상세 영역만 height animation으로 열고 닫는다.
- 조과 이미지가 없으면 사진 섹션 구분선을 숨긴다.
- 사진 확대 뷰어의 하단 페이지 컨트롤은 navigation bar inset과 최소 하단 여백을 반영한다.

## 지도 marker anchor 체크리스트

- 시작/종료 원형 마커: center anchor `(0.5, 0.5)`
- 낚시/탐색 pin 마커: bottom anchor `(0.5, 1.0)`
- 사진 썸네일 마커: 기존 Android 디자인 기준 유지
- Google/Kakao의 marker anchor 의미가 다를 수 있으므로 변경 시 양쪽을 모두 확인한다.
- 히스토리와 실시간 기록 화면의 anchor가 서로 달라지지 않게 맞춘다.

## 검증 체크리스트

- 공개 빌드에서 현재수온 OpenAPI 화면이 뜨는지 확인
- 공개 빌드에서 수온분석 탭이 준비중 화면인지 확인
- 공개 빌드에서 `/api/regions` 호출이 발생하지 않는지 확인
- 내부 빌드에서 crawling/직접 서버 호출 화면이 뜨는지 확인
- 내부 빌드 앱 시작 시 `/api/regions` 호출이 유지되는지 확인
- 내부 수온분석 상세 그래프 기준 드롭다운이 동작하는지 확인
- 날짜별 그래프는 가로 스크롤 없이 표시되는지 확인
- 12/6/3시간 그래프는 plot 영역만 가로 스크롤되는지 확인
- 시간별 그래프를 스크롤해도 왼쪽 온도 라벨이 고정되어 보이는지 확인
- 시간별 x축 라벨은 00시에만 날짜를 함께 표시하는지 확인
- 낚시기록 탭 최초 진입 안내 팝업 확인
- "다시 보지 않기" 저장 후 재진입 시 팝업이 안 뜨는지 확인
- 기록 시작 즉시 시작 마커가 보이는지 확인
- 기록 중 종료 마커가 현재 위치를 따라다니지 않는지 확인
- 기록 종료 후 경로와 임시 마커가 초기화되는지 확인
- 이동중에서 탐색/낚시로 바뀔 때만 상태 마커가 생기는지 확인
- 시작 직후 첫 상태가 탐색/낚시여도 히스토리에 불필요한 상태 마커가 생기지 않는지 확인
- 히스토리에서 시작/종료 마커 탭 시 하단 UI가 뜨는지 확인
- 히스토리 상단 accordion 확장/축소 시 UI가 겹치지 않는지 확인
- 사진 확대 뷰어 하단 컨트롤이 소프트키 영역과 겹치지 않는지 확인

## 빌드 명령

```bash
./gradlew assemblePublicDebug
./gradlew assembleInternalDebug
```

최근 검증 결과:

- `./gradlew assemblePublicDebug` 성공
- `./gradlew assembleInternalDebug` 성공
