---
description: Notion 업무 캘린더에 일일 작업 로그를 자동으로 기록하는 워크플로우
---

# Work Log

이 문서는 '온바다(SeaThermo)' Android 프로젝트의 일일 개발 로그를 Notion '업무 캘린더'에 자동으로 기록하기 위한 표준 매뉴얼입니다.
하루의 작업을 마무리할 때 이 가이드를 참고하여, 오늘 수행한 작업 내역을 요약하고 Notion 데이터베이스에 페이지를 생성해야 합니다.

## 1. 노션 데이터베이스 정보
- **이름**: `업무 캘린더`
- **ID**: `30412626-a195-80ad-8ea0-d28a0b7eb335`
- **URL**: `https://www.notion.so/30412626a19580ad8ea0d28a0b7eb335`

## 2. 속성(Properties) 매칭 규칙
Notion 페이지 생성(`create_page`) 시 `properties` 인자에 아래 규칙을 적용합니다.

### 📅 날짜 (Date)
- **속성명**: `날짜`
- **Notion Type**: `date`
- **값**: `Today (YYYY-MM-DD)`
- **설명**: 로그를 기록하는 당일 날짜 (예: `2026-02-11`)

### 📂 프로젝트 (Select)
- **속성명**: `프로젝트`
- **Notion Type**: `select`
- **값**: `온바다(AOS)`
- **설명**: Android 앱 개발 작업이므로 항상 `온바다(AOS)`를 선택합니다.

### 🏷 작업종류 (Multi-select)
- **속성명**: `작업종류`
- **Notion Type**: `multi_select`
- **선택 기준**: (오늘 수행한 작업 성격에 해당하는 태그를 모두 포함)
  - `UI/UX`: 화면 디자인, 레이아웃, 애니메이션 구현
  - `로직/기능`: 비즈니스 로직, 데이터 처리, 신규 기능(Feature) 개발
  - `버그수정`: 오류 해결, 크래시 수정
  - `리펙토링`: 코드 구조 개선, 변수명 변경, 최적화
  - `환경`: 프로젝트 설정, 라이브러리 관리, 배포 설정

### 📊 진행상황 (Status)
- **속성명**: `진행상황`
- **Notion Type**: `status`
- **값**:
  - `진행 중` (`In Progress`): 작업이 완료되지 않았거나 추가 작업이 필요한 경우
  - `완료` (`Done`): 계획된 작업이 모두 마무리된 경우

### 📝 이름 (Title)
- **속성명**: `이름`
- **Notion Type**: `title`
- **형식**: `[SeaThermo-AOS] YYYY-MM-DD (주요 작업 키워드)`
- **예시**: `[SeaThermo-AOS] 2026-03-16 Compose 메인 화면 구현`

---

## 3. 본문(Body) 기록 양식
페이지의 본문(`children`)은 아래 마크다운 구조를 따르며, Notion Block(Heading 2, Heading 3, Bullet list 등)으로 변환하여 작성합니다.

### 🚀 오늘 진행한 주요 작업 (Heading 2)
- **(작업 내용 요약)**: 오늘 수행한 핵심적인 개발 사항을 3~5줄 내외로 요약합니다. (Bullet list)
- **구체성**: 단순히 "수정함"보다는 "Compose LazyColumn으로 RecyclerView 대체 및 레거시 Adapter 삭제"와 같이 구체적으로 기술합니다.

### 🛠 상세 구현 내용 (Heading 2)
- **수정 파일 (Heading 3)**
  - `(수정된_주요_파일_경로.kt)` (Bullet list)
- **주요 로직 (Heading 3)**
  - **(함수/클래스명)**: 변경된 핵심 로직과 그 이유를 설명합니다. (Bullet list)

### 📝 비고 및 특이사항 (Heading 2)
- **이슈 (Heading 3)**
  - 해결하지 못한 버그나, 추가로 발견된 문제점을 기록합니다.
- **내일 할 일 (Heading 3)**
  - 내일 이어서 진행할 작업이나 확인해야 할 사항을 메모합니다.

---

## 4. 실행 절차
1. **작업 종료 시**: 사용자가 "오늘 작업 저장해줘" 또는 "로그 남겨줘"라고 요청하면 이 가이드를 로드합니다.
2. **Context 분석**: 최근 대화 내용과 수정된 파일 목록(`git status` 등 활용 가능)을 분석하여 오늘 날짜의 작업 내용을 정리합니다.
3. **Notion API 호출**:
   - `mcp_notion-mcp-server_API-post-page` 도구를 사용합니다.
   - 위 `2. 속성 매칭 규칙`에 맞춰 `properties` JSON을 구성합니다.
   - 위 `3. 본문 기록 양식`에 맞춰 `children` JSON을 구성합니다.
4. **결과 보고**: 생성된 Notion 페이지의 URL을 사용자에게 알려줍니다.
