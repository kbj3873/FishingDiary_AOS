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

### ⚠️ MCP 툴 제약사항 (반드시 숙지)
`API-patch-block-children` 툴은 아래 두 가지 블록 타입만 지원합니다.
- `paragraph` — 일반 텍스트 (섹션 제목에 사용)
- `bulleted_list_item` — 불릿 리스트

`heading_2`, `heading_3` 블록은 **지원하지 않습니다.**
섹션 제목은 이모지 + 텍스트 조합의 `paragraph` 블록으로 대체합니다.

### 본문 블록 구성 규칙

| 역할 | 블록 타입 | 예시 텍스트 |
|------|----------|------------|
| 대섹션 제목 | `paragraph` | `🚀 오늘 진행한 주요 작업` |
| 소섹션 제목 | `paragraph` | `▶ 신규 파일` |
| 항목 내용 | `bulleted_list_item` | `SettingScreen.kt — 설정 화면 구현` |

### 본문 섹션 구성
```
paragraph      → 🚀 오늘 진행한 주요 작업
bulleted_list  → 작업 내용 3~5줄 (구체적으로)

paragraph      → 🛠 상세 구현 내용
paragraph      → ▶ 신규 파일
bulleted_list  → 파일 경로
paragraph      → ▶ 수정 파일
bulleted_list  → 파일 경로 + 변경 내용
paragraph      → ▶ 주요 로직
bulleted_list  → 함수/클래스명: 핵심 로직 설명

paragraph      → 📝 비고 및 특이사항
paragraph      → ▶ 이슈
bulleted_list  → 해결하지 못한 버그, 발견된 문제점
paragraph      → ▶ 내일 할 일
bulleted_list  → 내일 이어서 진행할 작업
```

---

## 4. 실행 절차

> **핵심 원칙**: `API-post-page`의 `children` 파라미터는 동작하지 않습니다.
> 반드시 **2단계**로 분리하여 실행합니다.

### Step 1 — 페이지 생성 (`API-post-page`)
`properties`만 포함하여 페이지를 생성합니다. `children`은 절대 포함하지 않습니다.

```json
{
  "parent": { "type": "database_id", "database_id": "30412626-a195-80ad-8ea0-d28a0b7eb335" },
  "properties": { /* 2. 속성 매칭 규칙 참고 */ }
}
```

응답에서 생성된 페이지 `id`를 저장합니다.

### Step 2 — 본문 추가 (`API-patch-block-children`)
Step 1에서 받은 페이지 `id`를 `block_id`로 사용하여 본문 블록을 추가합니다.
블록 수가 많을 경우 섹션 단위로 나눠 여러 번 호출합니다.

```json
{
  "block_id": "<Step 1에서 받은 page id>",
  "children": [
    { "type": "paragraph", "paragraph": { "rich_text": [{ "type": "text", "text": { "content": "🚀 오늘 진행한 주요 작업" } }] } },
    { "type": "bulleted_list_item", "bulleted_list_item": { "rich_text": [{ "type": "text", "text": { "content": "작업 내용" } }] } }
  ]
}
```

### Step 3 — 결과 보고
생성된 Notion 페이지의 URL을 사용자에게 알려줍니다.
