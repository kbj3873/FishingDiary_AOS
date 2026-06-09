# 온바다 Android 작업 메모

이 문서는 SeaThermo Android 저장소로 가져가서 작업을 바로 시작하기 위한 README 초안이다. Android 저장소에는 현재 `CLAUDE.md`만 있다고 하므로, 이 파일과 `CODEX_Android.md`를 Android 저장소 루트에 두고 실제 코드 구조에 맞춰 보완하면 된다.

## 앱 개요

온바다(SeaThermo)는 낚시 활동을 기록하고 해양 수온 데이터를 확인하는 앱이다.

주요 기능:

- 현재/최신 해수 온도 확인
- 관측소 기반 현재수온 목록
- 수온분석 화면
- GPS 기반 낚시 경로 기록
- 속도 기반 낚시 상태 분류: 이동중, 탐색중, 낚시중
- 조과 사진 저장 및 지도 마커 표시
- 세션별 낚시 히스토리 조회
- 지도 타입 선택
- 앱 버전 체크, 공지사항, 오픈소스 라이선스 화면

## 현재 작업 목표

iOS에서 공개/내부 빌드 분리와 낚시기록/히스토리 UX 개선을 진행했다. Android에도 같은 정책과 동작을 맞추는 것이 목표다.

큰 방향:

1. 공개/내부 빌드 분리
2. 공개 빌드 현재수온은 공식 OpenAPI 사용
3. 공개 빌드 수온분석은 준비중 화면 표시
4. 내부 빌드는 기존 crawling/직접 서버 호출 사용
5. 내부 수온분석 상세 그래프 기준 선택/가로 스크롤 개선
6. 낚시기록 안내 팝업 추가
7. 낚시기록/히스토리에 시작/종료 마커 추가
8. 히스토리 마커/지점 수/상단 UI 동작을 iOS와 동일하게 정리

## 배포 전략

공개 Android 앱 package name:

```text
com.onbada.seathermo
```

주의사항:

- 공개 앱 package name은 바꾸지 않는다.
- 새 앱을 만들지 않는다.
- Play Console 테스트 조건을 다시 밟지 않도록 기존 앱에 더 높은 `versionCode`로 공개용 AAB를 올린다.
- 내부용은 별도 applicationId를 사용한다.

내부 앱 ID 권장:

```text
com.onbada.seathermo.internal
```

## 권장 빌드 모드

Android에서는 Gradle flavor 또는 build type으로 공개/내부를 나누고, 코드에서는 다음 flag를 기준으로 분기한다.

```kotlin
BuildConfig.INTERNAL_BUILD
```

예시:

```kotlin
if (BuildConfig.INTERNAL_BUILD) {
    // 내부용 crawling/직접 서버 호출
} else {
    // 공개용 OpenAPI 또는 준비중 화면
}
```

## Android 작업 시작 순서

1. Android 저장소의 `CLAUDE.md`를 읽는다.
2. `settings.gradle`, root `build.gradle`, app `build.gradle`을 확인한다.
3. package name, flavor/buildType, signing, versionCode 구조를 확인한다.
4. `BuildConfig.INTERNAL_BUILD` 또는 대응 flag를 추가할 위치를 정한다.
5. 현재수온, 수온분석, 낚시기록, 히스토리 관련 View/ViewModel/Repository를 찾는다.
6. 먼저 공개/내부 분기부터 잡고, 이후 낚시기록/히스토리 UX를 맞춘다.

검색 키워드:

```text
CurrentTemperature
OceanSelect
SeaAnalysis
FishingRecord
History
KakaoMap
GoogleMap
mapType
api/regions
region
BuildConfig
```

## iOS에서 확정된 동작

### 현재수온/수온분석

- 공개 빌드:
  - 현재수온은 공식 OpenAPI 기반 화면 사용
  - 수온분석은 준비중 화면 표시
- 내부 빌드:
  - crawling/직접 서버 호출 화면 사용
  - 앱 시작 시 `/api/regions` 호출 필요

### 내부 수온분석 상세 그래프

내부 빌드의 수온분석 상세 화면은 최근 7일 그래프를 다음 기준으로 전환할 수 있어야 한다.

선택 기준:

```text
날짜별
12시간
6시간
3시간
```

UI:

- `"최근 7일 수온 변화"` 오른쪽에 드롭다운 메뉴를 둔다.
- 선택 기준에 따라 subtitle을 변경한다.
- 날짜별은 기존 카드 폭에서 표시하고 가로 스크롤하지 않는다.
- 12/6/3시간 기준은 그래프를 가로 스크롤한다.
- 3시간 기준이 가장 넓은 스크롤 범위를 가진다.

그래프 처리:

- 7일 수온 데이터는 30분 단위 샘플을 그대로 사용한다.
- 기준 변경은 라인 데이터를 다시 샘플링하는 것이 아니라 x축 세로선/라벨 tick 간격을 바꾸는 것이다.
- 오른쪽으로 스크롤해도 왼쪽 온도 라벨은 고정되어 보여야 한다.
- 온도 라벨과 그래프 첫 세로선 사이 간격은 0에 가깝게 맞춘다.
- 최상단 온도 라벨이 잘리지 않도록 상단 여백을 둔다.

x축 라벨:

- 날짜별은 날짜만 표시한다. 예: `6/2`
- 시간별은 `00시`일 때만 날짜를 함께 표시한다. 예: `6/8 00시`
- 시간별의 나머지는 시간만 표시한다. 예: `03시`, `06시`
- 시간별 첫 라벨이 왼쪽에서 잘리지 않게 처리하되, 날짜별에는 이 보정을 적용하지 않는다.

### 낚시기록 안내 팝업

낚시기록 탭 진입 시 안내 팝업을 보여준다.

문구 취지:

```text
낚시 기록은 워킹 루어 낚시나 보트 낚시처럼 이동하며 포인트를 탐색하는 상황에 최적화되어 있습니다.
기록 중 이동 경로와 상태 변화를 저장하고, 사진으로 조과를 남겨 히스토리에서 다시 확인할 수 있습니다.
```

기능:

- 최초 또는 설정 저장값이 꺼져 있을 때 표시
- "다시 보지 않기" 제공
- preference key 예시: `hideFishingRecordGuidePopup`

### 시작/종료 마커

권장 asset 이름:

```text
ic_map_marker_start
ic_map_marker_end
```

동작:

- 기록 시작 시 시작 마커 표시
- 기록 종료 시 종료 마커 표시
- 낚시기록 화면과 히스토리 상세 화면 모두 표시
- 시작/종료 마커는 원형이므로 지도 marker anchor를 중앙으로 둔다.

anchor 기준:

```text
start/end marker: 0.5, 0.5
fishing/drifting pin marker: 0.5, 1.0
```

### 상태 마커 생성

상태 마커는 아래 전환에서만 생성한다.

```text
이동중 -> 탐색중
이동중 -> 낚시중
```

다음 보정은 하지 않는다.

- 시작 직후 탐색/낚시 상태라고 해서 강제 상태 마커 생성
- 탐색중/낚시중 사이 전환 마커 생성
- 탐색/낚시에서 이동중으로 바뀌는 마커 생성

### 히스토리 상세

- 시작 마커: 세션 첫 record에서 파생
- 종료 마커: 세션 마지막 record 또는 경로 마지막 좌표에서 파생
- 시작/종료 마커 탭 시 하단 UI 표시
  - 시작 제목: `낚시 시작`
  - 종료 제목: `낚시 종료`
- 사진이 없으므로 썸네일 없는 상태 마커 UI와 같은 구조를 재사용

### 히스토리 지점 수

목록과 상세에서 같은 기준으로 계산한다.

```text
시작 마커
+ 종료 마커, record가 2개 이상일 때
+ 이동중 -> 탐색/낚시 상태 마커
+ 사진 마커
```

시작 record는 상태 전환 판정에서 제외한다.

### 히스토리 상단 UI

상단 헤더 확장/축소는 accordion 방식으로 처리한다.

- 뒤로 버튼, 날짜, 시간, 삭제 버튼 영역은 고정
- 상세 영역만 height animation으로 확장/축소
- fade/move transition으로 위 헤더와 겹치지 않게 한다.
- 조과 사진이 없으면 사진 섹션 구분선도 숨긴다.

## Android 구현 시 확인할 위험 지점

- 내부/공개 flavor별 API base URL과 key가 올바른지
- 공개 빌드에 crawling/직접 호출 화면 또는 endpoint가 노출되지 않는지
- 내부 빌드에서 `/api/regions` 호출이 빠지지 않았는지
- 내부 수온분석 상세 그래프의 기준 선택, 스크롤, y축 고정 처리가 iOS와 맞는지
- 날짜별 그래프에 불필요한 첫 라벨 위치 보정이나 가로 스크롤이 들어가지 않았는지
- 시작/종료 마커가 경로 polyline과 같은 좌표 기준을 쓰는지
- 지도 SDK별 marker anchor 의미가 iOS와 다른지
- 히스토리의 마커 생성 로직과 목록의 지점 수 계산이 서로 다른지
- 오래된 기록 데이터에서 record 수가 1개인 경우 종료 마커/카운트가 깨지지 않는지

## 빌드/검증 명령 예시

실제 task 이름은 Android 저장소에서 확인한다.

```bash
./gradlew tasks
./gradlew assemblePublicDebug
./gradlew assembleInternalDebug
./gradlew test
./gradlew lint
```

최소 검증:

- 공개 debug 빌드 성공
- 내부 debug 빌드 성공
- 내부 수온분석 상세에서 날짜별/12시간/6시간/3시간 그래프 확인
- 시간별 그래프 스크롤 시 y축 온도 라벨 고정 확인
- 시간별 x축 라벨이 00시에만 날짜를 표시하는지 확인
- 낚시기록 화면 진입
- 기록 시작/종료
- 히스토리 상세 진입
- 마커 탭 하단 UI 확인
- 조과 이미지 없는 히스토리 상단 UI 확인
