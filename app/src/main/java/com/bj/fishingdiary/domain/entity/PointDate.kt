package com.bj.fishingdiary.domain.entity

import java.io.File

/**
 * 낚시 포인트 날짜 및 데이터 Entity
 * Fishing Point Date and Data Entities
 *
 * 로컬 파일 시스템에 저장된 낚시 기록을 관리하기 위한 데이터 모델
 * Data models for managing fishing records stored in local file system
 */

/**
 * 낚시 포인트 날짜 정보
 * Fishing Point Date Information
 *
 * 날짜별로 저장된 낚시 기록 폴더를 나타냄
 * Represents a fishing record folder stored by date
 *
 * 예시:
 * Example:
 * - date: "2024-01-15"
 * - datePath: /storage/emulated/0/FishingDiary/2024-01-15/
 *
 * @property date 낚시 날짜 (yyyy-MM-dd 형식)
 * @property datePath 날짜 폴더 경로
 */
data class PointDate(
    val date: String?,
    val datePath: File?  // Android에서는 java.io.File 사용 (iOS의 URL에 대응)
)

/**
 * 낚시 포인트 데이터 정보
 * Fishing Point Data Information
 *
 * 특정 날짜에 저장된 개별 낚시 기록 파일을 나타냄
 * Represents an individual fishing record file saved on a specific date
 *
 * 예시:
 * Example:
 * - dataName: "morning_fishing.json"
 * - dataPath: /storage/emulated/0/FishingDiary/2024-01-15/morning_fishing.json
 *
 * @property dataName 데이터 파일 이름
 * @property dataPath 데이터 파일 경로
 */
data class PointData(
    val dataName: String?,
    val dataPath: File?
)
