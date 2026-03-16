package com.onbada.seathermo.data.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room 데이터베이스 싱글턴.
 *
 * [개념] @Database 어노테이션으로 DB 설정을 선언합니다.
 *        - entities: 이 DB가 관리할 테이블(@Entity) 목록.
 *        - version: 스키마 버전. 테이블 구조 변경 시 올려야 합니다.
 *          iOS의 Realm.Configuration(schemaVersion: 3)에 대응합니다.
 *          (Android는 신규 구현이므로 version 1로 시작합니다.)
 *        - exportSchema: 스키마 JSON 파일 내보내기 여부. 팀 협업 시 true 권장.
 *
 * [개념] @TypeConverters는 DB 전체에 TypeConverter를 등록합니다.
 *        FishingRecordTypeConverters가 List<String> ↔ JSON 변환을 담당합니다.
 *
 * [개념] abstract class는 직접 인스턴스화할 수 없는 추상 클래스입니다.
 *        Room이 컴파일 타임에 구현체를 자동 생성합니다.
 *        iOS의 final class RealmManager와 달리 Room은 이 추상 클래스를 기반으로 동작합니다.
 */
@Database(
    entities = [FishingRecordEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(FishingRecordTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * FishingRecordDao 인스턴스를 반환합니다.
     *
     * [개념] abstract fun으로 선언하면 Room이 구현체를 자동 생성합니다.
     *        iOS의 RealmManager.shared.realm?.objects(T.Type)에 대응합니다.
     */
    abstract fun fishingRecordDao(): FishingRecordDao

    /**
     * 싱글턴 패턴 구현.
     *
     * [개념] companion object는 Swift의 static과 동일합니다.
     *        클래스 인스턴스 없이 AppDatabase.getInstance(context)로 호출합니다.
     *
     * [개념] @Volatile은 멀티스레드 환경에서 INSTANCE 변수의 최신 값을 항상 메인 메모리에서 읽도록 합니다.
     *        Swift의 nonisolated(unsafe) static var 또는 DispatchQueue로 보호하는 패턴에 대응합니다.
     *
     * [개념] synchronized(this) { }는 동시에 여러 스레드가 접근해도 한 번만 초기화되도록 잠금(lock)을 겁니다.
     *        Double-Checked Locking 패턴으로, Swift의 lazy var 스레드 안전성과 동일한 효과입니다.
     */
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    // [개념] applicationContext는 앱 전체 생명주기를 가진 Context입니다.
                    //        Activity Context 대신 applicationContext를 사용해 메모리 누수를 방지합니다.
                    context.applicationContext,
                    AppDatabase::class.java,
                    "seathermo_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
