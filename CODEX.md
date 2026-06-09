# CODEX_Android.md

이 문서는 Codex가 SeaThermo Android 저장소에서 작업을 바로 이어가기 위한 작업 가이드다. Android 저장소에는 `CLAUDE.md`만 있다고 가정한다. 작업을 시작하면 먼저 Android 저장소의 `CLAUDE.md`와 실제 Gradle/소스 구조를 확인하고, 내용이 충돌하면 실제 코드와 이 문서를 우선한다.

## 기본 원칙

- 사용자와의 소통, 작업 요약, 문서 작성은 한국어로 한다.
- 작업 전에는 요청과 관련된 파일을 먼저 읽고, 기존 Android 구조와 스타일에 맞춰 최소 범위로 수정한다.
- 사용자가 명시하지 않은 리팩토링, 포맷 변경, 파일 이동은 피한다.
- 기존 변경사항을 임의로 되돌리지 않는다.
- 빌드/테스트가 필요한 변경은 가능한 범위에서 직접 검증하고, 실행하지 못한 경우 이유를 남긴다.

## 현재 배포 전략

- 공개 앱 package name은 `com.onbada.seathermo`를 유지한다.
- Play Console에서 비공개 테스트 조건을 이미 통과한 앱이므로, 공개 배포용 안전 빌드는 같은 앱/package name에 더 높은 `versionCode`로 올리는 방향이 원칙이다.
- 새 앱을 만들거나 공개 package name을 바꾸면 테스트 요건을 다시 밟을 수 있으므로 피한다.
- 내부용 빌드는 별도 `applicationId` 또는 `applicationIdSuffix`를 사용한다.
  - 권장 예시: `com.onbada.seathermo.internal`
- git 브랜치를 장기적으로 public/private로 나누기보다, 하나의 코드베이스에서 Gradle flavor/buildConfig flag로 공개/내부 모드를 분리한다.

## 권장 빌드 분기 구조

Android에서는 iOS의 `INTERNAL_BUILD`와 동일한 의미의 플래그를 둔다.

권장 이름:

```kotlin
BuildConfig.INTERNAL_BUILD
```

권장 Gradle 방향:

```kotlin
productFlavors {
    create("public") {
        dimension = "distribution"
        applicationId = "com.onbada.seathermo"
        buildConfigField("boolean", "INTERNAL_BUILD", "false")
    }

    create("internal") {
        dimension = "distribution"
        applicationId = "com.onbada.seathermo.internal"
        buildConfigField("boolean", "INTERNAL_BUILD", "true")
    }
}
```

실제 Android 프로젝트가 이미 flavor를 쓰고 있으면 기존 네이밍과 구조를 우선한다. 새 flavor를 추가할 때는 signing, versionCode, Firebase/Crashlytics 설정, Kakao key, manifest placeholder도 함께 확인한다.

## 공개/내부 기능 분리 요구사항

### 공개 빌드

- 현재수온은 공식 OpenAPI 기반 구현을 사용한다.
- 국립수산과학원 서버 직접 호출 또는 온바다 내부 crawling용 직접 호출 UI가 공개 빌드에 노출되지 않게 한다.
- 수온분석 탭은 준비중 화면을 표시한다.
- 준비중 화면에는 히스토리 빈 상태에서 쓰는 아이콘/톤과 맞춰서 다음 취지의 문구를 넣는다.
  - "수온 분석 서비스는 준비중입니다."
  - 공개 배포에서만 해당하므로 `!BuildConfig.INTERNAL_BUILD` 조건으로 처리한다.

### 내부 빌드

- 기존 crawling/직접 서버 호출 구현을 사용한다.
- 앱 시작 시 온바다 서버의 `/api/regions` 호출이 필요하다.
- 내부 빌드에서 직접 호출 view/viewModel 또는 화면이 실행되도록 분기한다.

## Android에서 대응해서 찾아야 할 파일

파일명은 Android 저장소 구조에 따라 다를 수 있다. 작업 시작 시 `rg`로 대응 지점을 찾는다.

찾을 키워드:

```text
CurrentTemperature
OceanSelect
SeaAnalysis
History
FishingRecord
FishingHistory
RecordMap
KakaoMap
mapType
regions
api/regions
BuildConfig
```

우선 확인할 영역:

- 앱 진입/Splash: version check, region cache, onboarding/main routing
- Main tab/navigation: 현재수온, 수온분석, 낚시기록, 히스토리, 설정 탭
- 현재수온: OpenAPI 구현과 crawling/직접 호출 구현
- 수온분석: 공개 준비중 화면, 내부 분석 화면
- 낚시기록: 위치 추적, 속도 기반 상태 분류, 경로 저장, 사진 저장
- 히스토리: 세션 목록, 상세 지도, 마커 탭 하단 UI
- 지도: Google/Apple/Kakao 등 Android 프로젝트가 쓰는 지도 SDK별 marker anchor/click handling
- 저장소: Room/Realm/DataStore/SharedPreferences/Documents image files 등 실제 저장 방식

## iOS에서 완료된 변경사항을 Android에 반영할 항목

### 1. 내부 수온분석 상세 그래프

내부 빌드의 수온분석 상세 화면에서 최근 7일 그래프 표시 방식을 확장했다. Android도 같은 UX를 맞춘다.

기준 선택:

- 그래프 카드 상단 `"최근 7일 수온 변화"` 오른쪽에 기준 선택 드롭다운을 둔다.
- 선택 항목:
  - `날짜별`
  - `12시간`
  - `6시간`
  - `3시간`
- 선택 기준에 따라 subtitle을 변경한다.
  - 날짜별: `일별 수온 추이 분석`
  - 12시간: `12시간 단위 수온 추이 분석`
  - 6시간: `6시간 단위 수온 추이 분석`
  - 3시간: `3시간 단위 수온 추이 분석`

그래프 데이터/축 처리:

- 기존 7일 수온 데이터는 30분 단위 샘플을 유지한다.
- 기준 변경은 데이터를 다시 호출하거나 라인 데이터를 줄이는 기능이 아니다.
- 표층/중층/저층 라인은 모든 30분 샘플을 그대로 사용한다.
- 선택 기준은 x축 세로선과 하단 라벨 tick 간격만 바꾼다.
- 날짜별은 기존 카드 폭에서 표시하고 가로 스크롤하지 않는다.
- `12시간`, `6시간`, `3시간`은 가로 스크롤 처리한다.
- `3시간` 기준이 가장 넓은 스크롤 범위를 가진다.

x축 라벨 규칙:

- 날짜별: 날짜만 표시한다. 예: `6/2`, `6/3`
- 시간별: `00시` tick에서만 날짜를 함께 표시한다. 예: `6/8\n00시`
- 시간별의 나머지 tick은 시간만 표시한다. 예: `03시`, `06시`, `12시`
- 시간별 스크롤 첫 라벨은 왼쪽에서 잘리지 않게 보정하되, 너무 오른쪽으로 밀지 않는다.
- 날짜별은 스크롤뷰가 아니므로 첫 라벨 위치 보정을 적용하지 않는다.

y축/스크롤 처리:

- 시간별 그래프를 오른쪽으로 스크롤해도 왼쪽 온도 라벨은 고정되어야 한다.
- 온도 라벨과 그래프 첫 세로선 사이 가로 간격은 0에 가깝게 맞춘다.
- 그래프 최상단 온도 라벨이 잘리지 않도록 상단 padding을 확보한다.
- 구현 구조는 `고정 y축 영역 + 스크롤되는 plot 영역`이 가장 안전하다.

검증 포인트:

- `20.0` 같은 최상단 y축 라벨이 잘리지 않는지 확인한다.
- 오른쪽으로 스크롤해도 y축 온도 라벨이 계속 보이는지 확인한다.
- 날짜별 선택 시 그래프가 가로 스크롤되지 않는지 확인한다.
- 12/6/3시간 선택 시 그래프만 가로 스크롤되는지 확인한다.
- 시간별 첫 x축 라벨이 잘리지 않고, 그래프 왼쪽 기준과 어색하게 멀어지지 않는지 확인한다.

### 2. 낚시기록 안내 팝업

낚시기록 탭 진입 시 1회 안내 팝업을 표시한다.

안내 취지:

```text
낚시 기록은 워킹 루어 낚시나 보트 낚시처럼 이동하며 포인트를 탐색하는 상황에 최적화되어 있습니다.
기록 중 이동 경로와 상태 변화를 저장하고, 사진으로 조과를 남겨 히스토리에서 다시 확인할 수 있습니다.
```

요구사항:

- 탭 진입 시 사용자가 볼 수 있게 팝업 표시
- "다시 보지 않기" 기능 추가
- 저장 키는 Android 기존 preference 네이밍에 맞춘다.
  - 예시: `hideFishingRecordGuidePopup`
- 팝업 body font가 너무 작지 않게 조정한다.
- 기존 공통 팝업 컴포넌트가 있으면 확장해서 사용한다.

### 3. 낚시기록 시작/종료 마커

이미지 네이밍 권장:

```text
ic_map_marker_start
ic_map_marker_end
```

요구사항:

- 낚시 기록 화면에서도 시작 마커가 노출되어야 한다.
- 기록 시작 시 시작 마커를 추가한다.
- 기록 종료 시 종료 마커를 추가한다.
- 시작/종료 마커 이미지는 원형이므로 marker anchor는 정중앙이어야 한다.
  - Android 지도 SDK 기준 권장 anchor: `(0.5f, 0.5f)`
- 기존 낚시중/탐색중 pin 마커는 pin 끝점 기준을 유지한다.
  - 권장 anchor: `(0.5f, 1.0f)`
- 종료 마커는 경로 라인의 마지막 좌표에 찍혀야 한다.
  - 우선순위 예시: saved path 마지막 좌표 -> current map line/current location -> location manager latest

### 4. 상태 전환 마커 로직

현재 확정된 규칙:

- `이동중 -> 탐색중`
- `이동중 -> 낚시중`

위 두 경우에만 상태 마커를 생성한다.

하지 말아야 할 것:

- 시작 마커 직후 첫 상태가 탐색중/낚시중이라고 해서 강제로 상태 마커를 추가하지 않는다.
- `탐색중 -> 낚시중`, `낚시중 -> 탐색중`, `탐색/낚시 -> 이동중` 전환을 상태 마커로 세지 않는다.

히스토리 상세에서도 같은 규칙이어야 한다.

- 시작 record 또는 시작 boundary marker를 상태 전환의 이전 상태로 사용하지 않는다.
- 첫 실제 위치 상태는 baseline으로만 저장하고 마커를 만들지 않는다.
- 이후 `이동중 -> 탐색/낚시` 전환만 마커로 만든다.

### 5. 히스토리 시작/종료 마커

Realm/DB schema 변경 없이 가능한 경우:

- 세션 첫 record에서 시작 마커를 파생한다.
- 세션 마지막 record 또는 경로 마지막 좌표에서 종료 마커를 파생한다.

마커 탭 UI:

- 낚시중/탐색중 마커 탭 시 뜨는 하단 UI와 같은 구조를 재사용한다.
- 상태 마커 제목이 `"지점 #n"`이면, 시작/종료 마커는 다음으로 표시한다.
  - 시작: `"낚시 시작"`
  - 종료: `"낚시 종료"`
- 사진 썸네일은 없다.

### 6. 히스토리 지점 수 계산

목록과 상세의 지점 수 계산이 같아야 한다.

계산 기준:

```text
시작 마커 1
+ 종료 마커 1, 단 record가 2개 이상일 때
+ 이동중 -> 탐색/낚시 상태 마커 개수
+ 사진 마커 개수
```

첫 시작 record는 상태 전환 카운트에서 제외한다.

### 7. 히스토리 상단 UI accordion 개선

히스토리 상세 상단 UI의 확장/축소에서 상세 영역이 뒤로 버튼/날짜/시간/삭제 버튼과 겹치면 안 된다.

권장 방식:

- 헤더 영역은 항상 고정한다.
- 확장 상세 영역만 height animation으로 연다.
- fade in/out + move transition은 쓰지 않는다.
- 상세 영역에는 clipping을 적용한다.
- 조과 썸네일이 없는 경우, 조과 사진 섹션 위의 하단 구분선도 숨긴다.

주의:

- 중앙 날짜/시간/chevron 버튼이 좌우 버튼 레이어에 가려지지 않게 터치 영역과 z-order를 확인한다.

## 지도 marker anchor 체크리스트

- 시작/종료 원형 마커: center anchor `(0.5, 0.5)`
- 낚시/탐색 pin 마커: bottom anchor `(0.5, 1.0)`
- 사진 썸네일 마커: 기존 Android 디자인 기준 유지
- Apple/Kakao/iOS와 좌표 기준이 다를 수 있으므로 Android 지도 SDK 문서를 확인한다.
- 히스토리와 실시간 기록 화면의 anchor가 서로 달라지지 않게 맞춘다.

## 검증 체크리스트

- 공개 빌드에서 현재수온 OpenAPI 화면이 뜨는지 확인
- 공개 빌드에서 수온분석 탭이 준비중 화면인지 확인
- 내부 빌드에서 crawling/직접 서버 호출 화면이 뜨는지 확인
- 내부 빌드 앱 시작 시 `/api/regions` 호출이 유지되는지 확인
- 내부 수온분석 상세 그래프 기준 드롭다운이 동작하는지 확인
- 날짜별 그래프는 가로 스크롤 없이 표시되는지 확인
- 12/6/3시간 그래프는 가로 스크롤되고, 3시간 기준이 가장 넓은지 확인
- 시간별 그래프를 스크롤해도 왼쪽 온도 라벨이 고정되어 보이는지 확인
- 시간별 x축 라벨은 00시에만 날짜를 함께 표시하는지 확인
- 낚시기록 탭 최초 진입 안내 팝업 확인
- "다시 보지 않기" 저장 후 재진입 시 팝업이 안 뜨는지 확인
- 기록 시작 즉시 시작 마커가 보이는지 확인
- 기록 종료 시 경로 마지막 좌표에 종료 마커가 보이는지 확인
- 이동중에서 탐색/낚시로 바뀔 때만 상태 마커가 생기는지 확인
- 시작 직후 첫 상태가 탐색/낚시여도 히스토리에 불필요한 상태 마커가 생기지 않는지 확인
- 히스토리에서 시작/종료 마커 탭 시 하단 UI가 뜨는지 확인
- 히스토리 상단 accordion 확장/축소 시 UI가 겹치지 않는지 확인
- 조과 이미지가 없을 때 히스토리 상단 상세 영역 하단 구분선이 숨겨지는지 확인

## 빌드 명령 예시

실제 task 이름은 Android 저장소에서 확인한다.

```bash
./gradlew assemblePublicDebug
./gradlew assembleInternalDebug
./gradlew test
./gradlew lint
```

flavor가 없다면 먼저 `./gradlew tasks`로 사용 가능한 task를 확인한다.
