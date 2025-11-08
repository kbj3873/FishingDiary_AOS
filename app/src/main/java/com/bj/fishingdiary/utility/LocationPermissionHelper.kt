package com.bj.fishingdiary.utility

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * 위치 권한 상태
 * Location Permission Status
 *
 * iOS의 SCPermissionStatus에 대응
 */
enum class LocationPermissionStatus {
    DENIED,             // 거부됨
    GRANTED,            // 허용됨
    NOT_DETERMINED,     // 아직 결정되지 않음
    DONT_USE            // 사용할 수 없음
}

/**
 * 위치 권한 헬퍼
 * Location Permission Helper
 *
 * iOS의 SCPrivacy.checkLocation()에 대응
 * Android 버전별 위치 권한 체크 및 요청 로직
 */
object LocationPermissionHelper {

    /**
     * 기본 위치 권한 (FINE_LOCATION) 체크
     * Check basic location permission
     *
     * @param context Context
     * @return 권한 허용 여부
     */
    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 백그라운드 위치 권한 체크 (Android 10+)
     * Check background location permission
     *
     * @param context Context
     * @return 권한 허용 여부
     */
    fun hasBackgroundLocationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 10 미만에서는 기본 위치 권한으로 백그라운드도 허용됨
            true
        }
    }

    /**
     * 알림 권한 체크 (Android 13+)
     * Check notification permission
     *
     * @param context Context
     * @return 권한 허용 여부
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 13 미만에서는 알림 권한이 자동 허용됨
            true
        }
    }

    /**
     * 모든 필요한 권한이 허용되었는지 체크
     * Check if all required permissions are granted
     *
     * @param context Context
     * @param includeBackground 백그라운드 권한 포함 여부
     * @return 모든 권한 허용 여부
     */
    fun hasAllPermissions(context: Context, includeBackground: Boolean = true): Boolean {
        val hasLocation = hasLocationPermission(context)
        val hasBackground = if (includeBackground) {
            hasBackgroundLocationPermission(context)
        } else {
            true
        }
        val hasNotification = hasNotificationPermission(context)

        return hasLocation && hasBackground && hasNotification
    }

    /**
     * 요청해야 할 권한 목록 반환
     * Get permissions to request
     *
     * @param context Context
     * @param includeBackground 백그라운드 권한 포함 여부
     * @return 요청해야 할 권한 배열
     */
    fun getPermissionsToRequest(
        context: Context,
        includeBackground: Boolean = false
    ): Array<String> {
        val permissions = mutableListOf<String>()

        // 기본 위치 권한
        if (!hasLocationPermission(context)) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        // Android 10+ 백그라운드 위치 권한
        // 참고: 백그라운드 권한은 기본 위치 권한 이후 별도로 요청해야 함
        if (includeBackground &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !hasBackgroundLocationPermission(context) &&
            hasLocationPermission(context)  // 기본 권한이 있을 때만
        ) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        // Android 13+ 알림 권한
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasNotificationPermission(context)
        ) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return permissions.toTypedArray()
    }

    /**
     * 앱 설정 화면 열기
     * Open app settings
     *
     * iOS의 openApplicationSetting()에 대응
     *
     * @param context Context
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * 위치 권한 상태 확인
     * Check location permission status
     *
     * iOS의 SCPrivacy.checkLocation()에 대응
     *
     * @param context Context
     * @return 위치 권한 상태
     */
    fun checkLocationPermissionStatus(context: Context): LocationPermissionStatus {
        return when {
            hasLocationPermission(context) -> LocationPermissionStatus.GRANTED
            else -> LocationPermissionStatus.NOT_DETERMINED
        }
    }
}
