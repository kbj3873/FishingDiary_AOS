package com.bj.fishingdiary.managers

import android.content.Context
import android.util.Log
import com.bj.fishingdiary.data.storage.FileCreateResult
import com.bj.fishingdiary.data.storage.FileStorageError
import com.bj.fishingdiary.domain.entity.LocationData
import com.bj.fishingdiary.domain.entity.PointDate
import com.bj.fishingdiary.domain.entity.PointData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 파일 경로 타입 열거형
 * File Path Type Enumeration
 *
 * iOS의 FDFilePathType에 대응
 */
enum class FDFilePathType {
    POINT_PATH,     // point/
    TODAY_PATH;     // point/{오늘날짜}/

    /**
     * 파일 경로 반환
     * Returns file path
     */
    fun getPath(context: Context): String {
        val filesDir = context.filesDir  // Internal Storage: /data/data/com.bj.fishingdiary/files/
        return when (this) {
            POINT_PATH -> File(filesDir, "point").absolutePath
            TODAY_PATH -> File(filesDir, "point/${getTodayStringForPath()}").absolutePath
        }
    }

    /**
     * 파일 객체 반환
     * Returns File object
     */
    fun getFile(context: Context): File {
        return File(getPath(context))
    }

    companion object {
        /**
         * 오늘 날짜를 경로용 문자열로 반환
         * Returns today's date as path string
         *
         * 형식: yyyyMMdd (예: 20240115)
         */
        fun getTodayStringForPath(): String {
            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            return dateFormat.format(Date())
        }

        /**
         * 오늘 날짜를 저장용 문자열로 반환
         * Returns today's date as save string
         *
         * 형식: HHmmss (예: 143020)
         */
        fun getTodayStringForSave(): String {
            val dateFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
            return dateFormat.format(Date())
        }
    }
}

/**
 * 파일 관리자 클래스
 * File Manager Class
 *
 * iOS의 FDFileManager.swift에 대응
 * Internal Storage를 사용하여 파일 시스템 관리
 *
 * 중요: iOS Documents ≈ Android Internal Storage (context.filesDir)
 * Important: iOS Documents ≈ Android Internal Storage (context.filesDir)
 */
class FDFileManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "FDFileManager"

        @Volatile
        private var instance: FDFileManager? = null

        fun getInstance(context: Context): FDFileManager {
            return instance ?: synchronized(this) {
                instance ?: FDFileManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val gson = Gson()

    // ==================== 기본 함수들 ====================

    /**
     * 파일 경로 가져오기
     * Get file path
     */
    fun getFilePath(pathType: FDFilePathType): String {
        return pathType.getPath(context)
    }

    /**
     * 파일 URL 가져오기 (특정 이름 추가)
     * Get file URL with specific name
     */
    fun getFileUrl(pathType: FDFilePathType, nameType: String): File {
        return File(pathType.getPath(context), nameType)
    }

    /**
     * 경로 내 파일 목록 가져오기
     * Get file list in path
     */
    fun getFilePathList(pathType: FDFilePathType): List<String> {
        val file = pathType.getFile(context)
        return file.list()?.toList() ?: emptyList()
    }

    /**
     * URL의 파일 목록 가져오기
     * Get file list from URL
     */
    fun getFileList(url: File): List<String> {
        return url.list()?.toList() ?: emptyList()
    }

    /**
     * 경로 내 디렉토리 목록 가져오기
     * Get directory list in path
     */
    fun getDirPathList(pathType: FDFilePathType): List<File> {
        val file = pathType.getFile(context)
        if (!file.exists()) {
            return emptyList()
        }
        return file.listFiles()?.filter { it.isDirectory } ?: emptyList()
    }

    /**
     * URL의 디렉토리 목록 가져오기
     * Get directory list from URL
     */
    fun getDirList(pathUrl: File): List<File> {
        if (!pathUrl.exists()) {
            return emptyList()
        }
        return pathUrl.listFiles()?.filter { it.isDirectory } ?: emptyList()
    }

    /**
     * 경로에 디렉토리가 존재하는지 확인
     * Check if directories exist in path
     */
    fun existOfDirPath(pathType: FDFilePathType): Boolean {
        val fileList = getFilePathList(pathType)
        val exist = fileList.isNotEmpty()

        if (!exist) {
            Log.d(TAG, "not exist in path: ${getFilePath(pathType)}")
        } else {
            Log.d(TAG, "exist list count: ${fileList.size} in path: ${getFilePath(pathType)}")
        }

        return exist
    }

    // ==================== 디렉토리 생성/삭제 ====================

    /**
     * 기본 디렉토리 생성
     * Create default directories
     *
     * iOS의 createDefaultDirectories()에 대응
     * 앱 최초 실행 시 point/ 폴더 생성
     */
    fun createDefaultDirectories() {
        val pointPath = FDFilePathType.POINT_PATH.getFile(context)
        if (!pointPath.exists()) {
            val created = pointPath.mkdirs()
            if (created) {
                Log.d(TAG, "make directory: ${pointPath.absolutePath}")
            } else {
                Log.e(TAG, "failed to create directory: ${pointPath.absolutePath}")
            }
        }
    }

    /**
     * 파일 경로 생성
     * Create file path
     */
    fun createFilePath(pathType: FDFilePathType): FileCreateResult {
        val file = pathType.getFile(context)

        return if (!file.exists()) {
            try {
                val created = file.mkdirs()
                if (created) {
                    Result.success(true)
                } else {
                    Result.failure(FileStorageError.CreateError)
                }
            } catch (e: Exception) {
                Log.e(TAG, "createFilePath error", e)
                Result.failure(FileStorageError.CreateError)
            }
        } else {
            Result.failure(FileStorageError.CreateError)  // 이미 존재하면 에러
        }
    }

    /**
     * 파일 경로 생성 (이름 지정)
     * Create file path with name
     */
    fun createFilePath(pathType: FDFilePathType, pathName: String): FileCreateResult {
        val file = getFileUrl(pathType, pathName)

        return if (!file.exists()) {
            try {
                val created = file.mkdirs()
                if (created) {
                    Log.d(TAG, "create file path: ${file.absolutePath}")
                    Result.success(true)
                } else {
                    Result.failure(FileStorageError.CreateError)
                }
            } catch (e: Exception) {
                Log.e(TAG, "createFilePath error", e)
                Result.failure(FileStorageError.CreateError)
            }
        } else {
            Result.failure(FileStorageError.CreateError)  // 이미 존재하면 에러
        }
    }

    /**
     * 파일 쓰기
     * Write file
     */
    fun writeFilePath(
        data: ByteArray,
        pathType: FDFilePathType,
        fileName: String,
        completion: ((Boolean) -> Unit)? = null
    ) {
        val filePath = File(getFilePath(pathType), fileName)
        try {
            filePath.writeBytes(data)
            Log.d(TAG, "write succ file path: ${filePath.absolutePath}")
            completion?.invoke(true)
        } catch (e: Exception) {
            Log.e(TAG, "writeFilePath error", e)
            completion?.invoke(false)
        }
    }

    /**
     * 포인트 파일 쓰기
     * Write point file
     */
    fun writePointFilePath(
        data: ByteArray,
        dirNum: String,
        fileName: String,
        completion: ((Boolean) -> Unit)? = null
    ) {
        val filePath = File(FDFilePathType.TODAY_PATH.getPath(context))
            .let { File(it, dirNum) }
            .let { File(it, fileName) }

        try {
            filePath.writeBytes(data)
            Log.d(TAG, "write succ file path: ${filePath.absolutePath}")
            completion?.invoke(true)
        } catch (e: Exception) {
            Log.e(TAG, "writePointFilePath error", e)
            completion?.invoke(false)
        }
    }

    /**
     * 파일 삭제
     * Remove file
     */
    fun removeFilePath(pathType: FDFilePathType, nameType: String) {
        val filePath = getFileUrl(pathType, nameType)
        if (filePath.exists()) {
            try {
                Log.d(TAG, "delete file path: ${filePath.absolutePath}")
                filePath.deleteRecursively()
            } catch (e: Exception) {
                Log.e(TAG, "removeFilePath error", e)
            }
        }
    }

    /**
     * 포인트 파일 읽기
     * Get point file
     */
    fun getPointFile(pathUrl: File, fileName: String): List<LocationData>? {
        return try {
            val fileUrl = File(pathUrl, fileName)
            val json = fileUrl.readText()

            val type = object : TypeToken<List<LocationData>>() {}.type
            val pointList: List<LocationData> = gson.fromJson(json, type)
            pointList
        } catch (e: Exception) {
            Log.e(TAG, "getPointFile error", e)
            null
        }
    }

    // ==================== Public Data Repository 함수들 ====================

    /**
     * 포인트 날짜 폴더 생성
     * Create point date folder
     *
     * iOS의 createPointDate()에 대응
     */
    fun createPointDate(): FileCreateResult {
        return if (!existOfDirPath(FDFilePathType.TODAY_PATH)) {
            createFilePath(FDFilePathType.TODAY_PATH)
        } else {
            Result.failure(FileStorageError.CreateError)
        }
    }

    /**
     * 포인트 데이터 폴더 생성
     * Create point data folder
     *
     * iOS의 createPointData()에 대응
     */
    fun createPointData(): FileCreateResult {
        val dirList = getDirPathList(FDFilePathType.TODAY_PATH)
        val dirNum = dirList.size.toString()
        return createFilePath(FDFilePathType.TODAY_PATH, dirNum)
    }

    /**
     * 포인트 위치 데이터 저장
     * Save points location data
     *
     * iOS의 savePoints()에 대응
     */
    fun savePoints(locations: List<LocationData>) {
        try {
            val json = gson.toJson(locations)
            val jsonData = json.toByteArray(Charsets.UTF_8)

            val dirList = getDirPathList(FDFilePathType.TODAY_PATH)
            if (dirList.isNotEmpty()) {
                val dirNum = (dirList.size - 1).toString()
                val fileName = "${FDFilePathType.getTodayStringForSave()}.json"
                writePointFilePath(jsonData, dirNum, fileName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "savePoints error: json encode failed", e)
        }
    }

    /**
     * 포인트 날짜 목록 조회
     * Fetch point date list
     *
     * iOS의 fetchPointDateList()에 대응
     */
    fun fetchPointDateList(completion: (Result<List<PointDate>>) -> Unit) {
        if (existOfDirPath(FDFilePathType.POINT_PATH)) {
            val dirList = getDirPathList(FDFilePathType.POINT_PATH)

            if (dirList.isNotEmpty()) {
                val pointDates = dirList.map { dir ->
                    PointDate(
                        date = dir.name,
                        datePath = dir
                    )
                }.sortedBy { it.date }

                completion(Result.success(pointDates))
            } else {
                completion(Result.failure(FileStorageError.ReadError))
            }
        } else {
            completion(Result.failure(FileStorageError.ReadError))
        }
    }

    /**
     * 포인트 데이터 목록 조회
     * Fetch point data list
     *
     * iOS의 fetchPointDataList()에 대응
     */
    fun fetchPointDataList(pointDate: PointDate, completion: (Result<List<PointData>>) -> Unit) {
        val dirPath = pointDate.datePath
        if (dirPath == null) {
            completion(Result.failure(FileStorageError.ReadError))
            return
        }

        val pointDirList = getDirList(dirPath)
        if (pointDirList.isNotEmpty()) {
            val pointList = pointDirList.map { point ->
                PointData(
                    dataName = point.name,
                    dataPath = point
                )
            }.sortedBy { data ->
                data.dataName?.toIntOrNull() ?: 0
            }

            completion(Result.success(pointList))
        } else {
            completion(Result.failure(FileStorageError.ReadError))
        }
    }

    /**
     * 위치 데이터 목록 조회
     * Fetch locations
     *
     * iOS의 fetchLocations()에 대응
     */
    fun fetchLocations(pointData: PointData, completion: (Result<List<LocationData>>) -> Unit) {
        val path = pointData.dataPath
        if (path == null) {
            completion(Result.failure(FileStorageError.ReadError))
            return
        }

        val fileList = getFileList(path)
        val list = mutableListOf<LocationData>()

        if (fileList.isNotEmpty()) {
            for (file in fileList) {
                val pointList = getPointFile(path, file)
                if (pointList != null) {
                    list.addAll(pointList)
                }
            }

            val locations = list.sortedBy { it.sequence }
            completion(Result.success(locations))
        } else {
            completion(Result.failure(FileStorageError.ReadError))
        }
    }
}
