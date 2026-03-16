# 커밋 메시지 생성 규칙

이 파일을 로드하면 아래 규칙에 따라 커밋 메시지를 자동 생성할 수 있습니다.

---

## 제목 형식

```
{타입}: {한 줄 요약}
```

**타입 목록**

| 타입 | 사용 시점 |
|---|---|
| `Feat` | 신규 기능 추가 |
| `Refactor` | 코드 구조 개편 (기능 변경 없음) |
| `Fix` | 버그 수정 |
| `Remove` | 파일/코드 삭제 |
| `Build` | 빌드 설정 변경 (gradle, libs.versions.toml 등) |
| `Docs` | 문서/가이드 추가 및 수정 |
| `Chore` | 기타 잡무 (설정 파일, .gitignore 등) |

---

## 본문 섹션 구성

변경 내용을 아래 섹션 중 해당하는 것만 골라서 작성합니다.

```
[레거시 제거]
- 삭제한 파일 또는 코드 목록

[Domain 레이어]
- 추가/수정된 Entity, Repository 인터페이스, UseCase

[Data 레이어]
- 추가/수정된 DTO, Room DB, Repository 구현체

[Infrastructure]
- network, webview 등 저수준 레이어 변경 사항

[Presentation]
- Screen, ViewModel, Component 변경 사항

[DI]
- DI 컨테이너 및 팩토리 메서드 변경 사항

[빌드 설정]
- build.gradle.kts, libs.versions.toml 변경 사항

[프로젝트 가이드]
- CLAUDE.md, .agent/ 문서 변경 사항
```

---

## 꼬리말

```
Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
```

---

## 생성 예시 (이 세션 기준)

```
Refactor: Compose 전환 기반 작업 — 레거시 제거 및 Clean Architecture 신규 구현

[레거시 제거]
- XML 기반 Activity / Adapter / Layout 파일 전체 삭제
- 구 UseCase / Repository 삭제 (Ocean/Point/Track/SeaWaterTemperature)
- 불필요한 추상화 제거: DataTransferService, RepositoryTask, NetworkConfig 등
- 구 DI 컨테이너 삭제: DataServiceDIContainer, MainSceneDIContainer
- FDFileManager, FileDataStorage 삭제

[Domain 레이어]
- Entity 신규: CurrentTemperature, WeeklyTemperature, Region, VersionStatus, FishingRecord
- Repository 인터페이스: OceanRepository, SplashRepository, FishingRecordRepository
- UseCase: OceanUseCase, SplashUseCase, FishingRecordUseCase

[Data 레이어]
- DTO 신규: RisaListDTO, RisaInfoDTO, RegionDTO, VersionCheckDTO
- Room DB 신규: AppDatabase, FishingRecordEntity, FishingRecordDao, TypeConverters
- Repository 구현체: DefaultOceanRepository, DefaultSplashRepository, DefaultFishingRecordRepository

[Infrastructure]
- Endpoint.kt: baseURL 포함, url()/urlRequest() 추가, 불필요한 인터페이스 제거
- DefaultNetworkService.kt: suspend 전환, SessionManager 제거
- WebViewConfig.kt 신규: WebPage enum (NOTICES, LICENSES)

[DI]
- ApplicationDIContainer 신규: NIFS + OnBada 이중 NetworkService, 팩토리 메서드 전체
- AppConfiguration: apiNifsURL + apiOnbadaURL 이중 URL

[빌드 설정]
- Compose BOM, Navigation, Lifecycle, Room, KSP 의존성 추가
- ONBADA_BASE_URL BuildConfig 추가

[프로젝트 가이드]
- CLAUDE.md, .agent/workflows/ 추가

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
```
