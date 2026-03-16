package com.onbada.seathermo.data.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * 낚시 기록 DAO (Data Access Object).
 *
 * [개념] @Dao 어노테이션이 붙은 interface는 Room이 컴파일 타임에 구현 코드를 자동 생성합니다.
 *        개발자는 SQL 쿼리만 선언하면 되고, 실제 DB 접근 코드는 Room이 만들어 줍니다.
 *        iOS의 Realm fetch/write 호출을 대체합니다.
 *
 * [개념] Realm은 RealmManager 한 곳에서 모든 CRUD를 처리하지만,
 *        Room은 테이블(Entity)마다 DAO를 분리해 역할을 명확히 구분합니다.
 */
@Dao
interface FishingRecordDao {

    /**
     * 기록 1건을 저장합니다.
     *
     * [개념] @Insert는 INSERT SQL을 자동 생성합니다.
     *        OnConflictStrategy.REPLACE: 동일한 PrimaryKey가 있으면 덮어씁니다.
     *        iOS의 realm.add(object)에 대응합니다.
     *
     * [개념] suspend fun은 코루틴 안에서 호출하는 비동기 함수입니다.
     *        Room은 suspend fun을 선언하면 자동으로 백그라운드 스레드에서 실행합니다.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: FishingRecordEntity)

    /**
     * 특정 날짜 범위의 기록을 조회합니다.
     *
     * [개념] @Query에 SQL 문자열을 직접 작성합니다.
     *        :startOfDay, :endOfDay처럼 콜론(:)으로 함수 파라미터를 SQL에 바인딩합니다.
     *        iOS의 realm.objects(T.Type).filter("date >= %@", startDate)에 대응합니다.
     *
     * @param startOfDay 해당 날짜 시작 시각 (Unix timestamp, ms).
     * @param endOfDay   해당 날짜 종료 시각 (Unix timestamp, ms).
     */
    @Query("SELECT * FROM fishing_records WHERE date >= :startOfDay AND date < :endOfDay ORDER BY date ASC")
    suspend fun getByDate(startOfDay: Long, endOfDay: Long): List<FishingRecordEntity>

    /**
     * 전체 기록을 최신순으로 조회합니다 (히스토리 화면용).
     */
    @Query("SELECT * FROM fishing_records ORDER BY date DESC")
    suspend fun getAll(): List<FishingRecordEntity>

    /**
     * 특정 세션의 모든 기록을 삭제합니다.
     *
     * [개념] @Query로 DELETE문도 작성할 수 있습니다.
     *        iOS의 realm.delete(objects.filter("sessionId == %@", sessionId))에 대응합니다.
     */
    @Query("DELETE FROM fishing_records WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)

    /**
     * ID 목록으로 여러 기록을 일괄 삭제합니다.
     *
     * [개념] IN (:ids)는 SQL의 IN 절입니다.
     *        Room은 List<String> 파라미터를 자동으로 IN 절로 변환합니다.
     */
    @Query("DELETE FROM fishing_records WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    /**
     * 단일 기록을 ID로 삭제합니다 (사진 마커 개별 삭제용).
     */
    @Query("DELETE FROM fishing_records WHERE id = :id")
    suspend fun deleteById(id: String)
}
