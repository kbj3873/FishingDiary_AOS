package com.onbada.seathermo.domain.entity

import java.util.UUID

/**
 * 위도/경도 좌표를 담는 값 객체.
 *
 * [개념] iOS는 튜플 `(latitude: Double, longitude: Double)`을 지원하지만
 *        Kotlin에는 튜플이 없습니다. 대신 data class로 동일한 역할을 합니다.
 *        data class는 equals()와 hashCode()를 자동으로 생성하므로
 *        좌표 비교도 값 기반으로 동작합니다.
 *
 * @property latitude 위도.
 * @property longitude 경도.
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)

/**
 * 낚시 기록 엔티티.
 *
 * iOS의 FishingRecord struct와 동일합니다.
 *
 * [개념] iOS의 Identifiable 프로토콜 → Kotlin에서는 별도 구현 없이 id 프로퍼티로 대체합니다.
 *        iOS의 Codable → Kotlin에서는 Gson의 @SerializedName 또는 직렬화 로직을 Data 레이어에서 처리합니다.
 *        도메인 엔티티는 직렬화 프레임워크에 의존하지 않아야 하므로 어노테이션을 사용하지 않습니다.
 *
 * @property id 기록 고유 ID.
 *              [개념] UUID.randomUUID().toString()은 iOS의 UUID().uuidString과 동일합니다.
 * @property sessionId 낚시 1회를 구분하는 세션 ID.
 * @property date 기록 일시 (Unix 타임스탬프 밀리초).
 *                [개념] iOS의 Date 타입 대신 Android에서는 Long(밀리초)을 사용합니다.
 * @property location 위도/경도 좌표.
 * @property speed 이동 속도 (km/h).
 * @property state 상태 코드 (0: 이동, 1: 탐색, 2: 낚시).
 * @property imagePaths 첨부 이미지 파일 경로 목록.
 */
data class FishingRecord(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String = "",
    val date: Long,
    val location: GeoPoint,
    val speed: Double,
    val state: Int = 0,
    val imagePaths: List<String> = emptyList()
)
