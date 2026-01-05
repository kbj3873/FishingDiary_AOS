package com.bj.fishingdiary.common.extensions

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Date 확장 함수
 * Date Extension Functions
 *
 * iOS의 Date.swift와 동일한 기능을 제공
 * Provides same functionality as iOS Date.swift
 */

/**
 * Date 유틸리티 객체
 * Date Utility Object
 */
object DateUtils {
    /**
     * 오늘 날짜를 지정된 형식으로 반환
     * Returns today's date in specified format
     */
    fun todayString(format: String = "yyyy-MM-dd HH:mm:ss"): String {
        val dateFormatter = SimpleDateFormat(format, Locale("ko", "KR"))
        return dateFormatter.format(Date())
    }

    /**
     * 수온 데이터 조회 시작 날짜 (오늘로부터 6일 전)
     * Start date for temperature data query (6 days before today)
     */
    fun startTempDateString(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -6)
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale("ko", "KR"))
        return dateFormatter.format(calendar.time)
    }

    /**
     * 수온 데이터 조회 종료 날짜 (오늘)
     * End date for temperature data query (today)
     */
    fun endTempDateString(): String {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale("ko", "KR"))
        return dateFormatter.format(Date())
    }
}

/**
 * 날짜를 타임스탬프 형식으로 반환 (yyyyMMddHHmm)
 * Returns date in timestamp format (yyyyMMddHHmm)
 */
fun Date.tempDateString(): String {
    val dateFormatter = SimpleDateFormat("yyyyMMddHHmm", Locale("ko", "KR"))
    return dateFormatter.format(this)
}

/**
 * 문자열을 Date 객체로 변환
 * Convert string to Date object
 *
 * @param format 날짜 형식 (기본값: "yyyyMMddHHmm")
 * @return Date 객체 또는 null
 */
fun String.toDate(format: String = "yyyyMMddHHmm"): Date? {
    return try {
        val dateFormatter = SimpleDateFormat(format, Locale("ko", "KR"))
        dateFormatter.parse(this)
    } catch (e: Exception) {
        null
    }
}

/**
 * 문자열의 부분 문자열 추출
 * Extract substring
 *
 * @param from 시작 인덱스 (포함)
 * @param to 종료 인덱스 (포함)
 * @return 부분 문자열
 */
fun String.subStrings(from: Int, to: Int): String {
    if (from >= this.length || to >= this.length || from > to) {
        return ""
    }
    return this.substring(from, to + 1)
}
