package com.bj.fishingdiary.common.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * SharedPreferences 유틸리티 클래스
 * SharedPreferences Utility Class
 *
 * iOS의 FDUserDefaults와 동일한 역할
 * - UserDefaults.standard -> SharedPreferences
 * - Codable -> Gson (JSON 직렬화/역직렬화)
 *
 * 사용 예시:
 * ```kotlin
 * // 리스트 저장
 * FDUserDefaults.setToList(context, oceanStations, UserDefaultKey.REGIONAL_SEA_TEMPERATURE_LIST)
 *
 * // 리스트 불러오기
 * val stations = FDUserDefaults.getFromList<OceanStationModel>(context, UserDefaultKey.REGIONAL_SEA_TEMPERATURE_LIST)
 * ```
 */
object FDUserDefaults {

    private const val PREFERENCE_NAME = "FishingDiaryPreferences"

    /**
     * SharedPreferences 인스턴스 가져오기
     * Get SharedPreferences instance
     */
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Gson 인스턴스 (JSON 변환용)
     * Gson instance for JSON conversion
     */
    val gson = Gson()

    // ==================== 기본 타입 저장/불러오기 ====================

    /**
     * 문자열 저장
     */
    fun setString(context: Context, key: String, value: String?) {
        getPreferences(context).edit().putString(key, value).apply()
    }

    /**
     * 문자열 불러오기
     */
    fun getString(context: Context, key: String, defaultValue: String? = null): String? {
        return getPreferences(context).getString(key, defaultValue)
    }

    /**
     * 정수 저장
     */
    fun setInt(context: Context, key: String, value: Int) {
        getPreferences(context).edit().putInt(key, value).apply()
    }

    /**
     * 정수 불러오기
     */
    fun getInt(context: Context, key: String, defaultValue: Int = 0): Int {
        return getPreferences(context).getInt(key, defaultValue)
    }

    /**
     * Boolean 저장
     */
    fun setBoolean(context: Context, key: String, value: Boolean) {
        getPreferences(context).edit().putBoolean(key, value).apply()
    }

    /**
     * Boolean 불러오기
     */
    fun getBoolean(context: Context, key: String, defaultValue: Boolean = false): Boolean {
        return getPreferences(context).getBoolean(key, defaultValue)
    }

    /**
     * Float 저장
     */
    fun setFloat(context: Context, key: String, value: Float) {
        getPreferences(context).edit().putFloat(key, value).apply()
    }

    /**
     * Float 불러오기
     */
    fun getFloat(context: Context, key: String, defaultValue: Float = 0f): Float {
        return getPreferences(context).getFloat(key, defaultValue)
    }

    /**
     * Long 저장
     */
    fun setLong(context: Context, key: String, value: Long) {
        getPreferences(context).edit().putLong(key, value).apply()
    }

    /**
     * Long 불러오기
     */
    fun getLong(context: Context, key: String, defaultValue: Long = 0L): Long {
        return getPreferences(context).getLong(key, defaultValue)
    }

    /**
     * 키 삭제
     */
    fun removeObject(context: Context, key: String) {
        getPreferences(context).edit().remove(key).apply()
    }

    /**
     * 모든 데이터 삭제
     */
    fun clear(context: Context) {
        getPreferences(context).edit().clear().apply()
    }

    // ==================== 리스트 저장/불러오기 (JSON 변환) ====================

    /**
     * 모델 리스트를 JSON으로 변환하여 저장
     * Save model list by converting to JSON
     *
     * iOS의 setToList<T: Codable>와 동일한 기능
     *
     * @param context Context
     * @param values 저장할 리스트
     * @param key 저장 키
     */
    inline fun <reified T> setToList(context: Context, values: List<T>?, key: String) {
        try {
            val json = gson.toJson(values)
            setString(context, key, json)
        } catch (e: Exception) {
            println("setToList: model to json data encoded failed - ${e.message}")
        }
    }

    /**
     * JSON에서 모델 리스트로 변환하여 불러오기
     * Load model list by converting from JSON
     *
     * iOS의 getFromList<T: Codable>와 동일한 기능
     *
     * @param context Context
     * @param key 불러올 키
     * @return 모델 리스트 (없으면 빈 리스트)
     */
    inline fun <reified T> getFromList(context: Context, key: String): List<T> {
        try {
            val json = getString(context, key)
            if (json.isNullOrEmpty()) {
                println("getFromList: empty data")
                return emptyList()
            }

            // TypeToken을 사용하여 제네릭 타입 정보 보존
            val type = object : TypeToken<List<T>>() {}.type
            return gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            println("getFromList: model to json data decoded failed - ${e.message}")
            return emptyList()
        }
    }
}

/**
 * UserDefaults 키 상수 정의
 * UserDefaults Key Constants
 *
 * iOS의 UserDefaultKey와 동일한 역할
 */
object UserDefaultKey {
    /**
     * 메인화면 노출되는 수온 지역 리스트
     * Regional sea temperature list displayed on main screen
     */
    const val REGIONAL_SEA_TEMPERATURE_LIST = "regionalSeaTempuratureList"
}
