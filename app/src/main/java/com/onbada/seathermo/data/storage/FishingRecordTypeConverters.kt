package com.onbada.seathermo.data.storage

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room TypeConverter — List<String> ↔ JSON 문자열 변환.
 *
 * [개념] Room은 기본 타입(Int, Long, String 등)만 직접 저장할 수 있습니다.
 *        List, data class 같은 복합 타입은 TypeConverter로 저장 가능한 타입으로 변환해야 합니다.
 *        AppDatabase의 @TypeConverters 어노테이션에 등록하면 자동으로 적용됩니다.
 *
 * iOS의 Realm List<String>은 기본 지원이지만,
 * Room에서는 이 변환기를 통해 동일한 기능을 구현합니다.
 *
 * 저장 형태: ["path/a.jpg", "path/b.jpg"] → "[\"path/a.jpg\",\"path/b.jpg\"]" (JSON 문자열)
 */
class FishingRecordTypeConverters {

    private val gson = Gson()

    /**
     * JSON 문자열 → List<String> 변환 (DB 읽기 시 자동 호출).
     *
     * [개념] @TypeConverter 어노테이션이 붙은 함수는 Room이 자동으로 호출합니다.
     *        파라미터 타입(String) → 반환 타입(List<String>) 방향으로 변환합니다.
     *
     * [개념] TypeToken은 Gson에서 제네릭 타입 정보를 런타임에 전달할 때 사용합니다.
     *        List<String>처럼 제네릭이 포함된 타입은 런타임에 타입 정보가 소거(Type Erasure)되므로
     *        TypeToken으로 감싸서 정확한 타입을 알려줍니다.
     */
    @TypeConverter
    fun fromJson(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    /**
     * List<String> → JSON 문자열 변환 (DB 쓰기 시 자동 호출).
     *
     * [개념] null-safe 처리: list가 null이면 빈 배열 JSON("[]")을 저장합니다.
     *        Kotlin의 ?: 연산자로 간결하게 표현합니다.
     */
    @TypeConverter
    fun toJson(list: List<String>?): String {
        return gson.toJson(list ?: emptyList<String>())
    }
}
