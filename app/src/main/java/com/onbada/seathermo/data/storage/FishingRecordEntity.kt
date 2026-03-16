package com.onbada.seathermo.data.storage

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.onbada.seathermo.domain.entity.FishingRecord
import com.onbada.seathermo.domain.entity.GeoPoint
import java.util.UUID

/**
 * 낚시 기록 Room Entity (DB 테이블 정의).
 *
 * [개념] @Entity는 이 data class가 Room DB의 테이블임을 선언합니다.
 *        tableName으로 실제 SQL 테이블 이름을 지정합니다.
 *        iOS의 RealmFishingRecord: Object에 대응합니다.
 *
 * [개념] Realm은 클래스 자체가 DB 객체이지만,
 *        Room은 순수 data class에 어노테이션을 붙여 테이블을 정의합니다.
 *        따라서 도메인 Entity(FishingRecord)와 별도로 DB 전용 Entity를 만드는 것이 관례입니다.
 *
 * @property id         기록 고유 ID (Primary Key).
 *                      [개념] @PrimaryKey는 테이블의 기본 키를 지정합니다. SQL의 PRIMARY KEY와 동일합니다.
 * @property sessionId  낚시 1회 세션을 구분하는 ID.
 * @property date       기록 시각 (Unix timestamp, milliseconds).
 *                      [개념] iOS의 Date → Android Long.
 *                             Room은 Date 타입을 직접 저장할 수 없어 Long으로 변환합니다.
 * @property latitude   위도.
 * @property longitude  경도.
 * @property speed      속도 (km/h).
 * @property state      포인트 상태 (0: 이동 중, 1: 탐색, 2: 낚시).
 * @property imagePaths 첨부 사진 파일 경로 목록.
 *                      [개념] Room은 List<String>을 직접 저장할 수 없습니다.
 *                             AppDatabase에 등록된 TypeConverter가 JSON 문자열로 변환해 저장합니다.
 *                             iOS의 @Persisted var imagePaths = List<String>()에 대응합니다.
 */
@Entity(tableName = "fishing_records")
data class FishingRecordEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String = "",
    val date: Long,
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val state: Int = 0,
    val imagePaths: List<String> = emptyList()
) {
    /**
     * Entity를 도메인 모델로 변환합니다.
     *
     * [개념] iOS의 toDomain() func에 대응합니다.
     *        latitude/longitude 두 값을 GeoPoint로 묶습니다.
     *        iOS는 tuple (latitude, longitude)를 사용했지만,
     *        Kotlin에는 tuple이 없으므로 data class GeoPoint로 대응합니다.
     */
    fun toDomain(): FishingRecord = FishingRecord(
        id = id,
        sessionId = sessionId,
        date = date,
        location = GeoPoint(latitude = latitude, longitude = longitude),
        speed = speed,
        state = state,
        imagePaths = imagePaths
    )

    companion object {
        /**
         * 도메인 모델로부터 Entity를 생성합니다.
         *
         * [개념] companion object는 Swift의 static func에 대응합니다.
         *        인스턴스 없이 FishingRecordEntity.from(record)처럼 호출합니다.
         */
        fun from(record: FishingRecord): FishingRecordEntity = FishingRecordEntity(
            id = record.id,
            sessionId = record.sessionId,
            date = record.date,
            latitude = record.location.latitude,
            longitude = record.location.longitude,
            speed = record.speed,
            state = record.state,
            imagePaths = record.imagePaths
        )
    }
}
