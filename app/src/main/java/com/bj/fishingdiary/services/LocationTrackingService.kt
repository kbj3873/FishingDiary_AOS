package com.bj.fishingdiary.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bj.fishingdiary.R
import com.bj.fishingdiary.managers.FDLocationManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/**
 * 위치 추적 Foreground Service
 * Location Tracking Foreground Service
 *
 * 백그라운드에서도 위치를 지속적으로 추적하기 위한 서비스
 * FusedLocationProviderClient를 사용하여 배터리 효율적으로 위치 추적
 */
class LocationTrackingService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val CHANNEL_NAME = "위치 추적"

        // LocationRequest 설정
        private const val UPDATE_INTERVAL_MS = 1000L        // 1초마다 업데이트
        private const val FASTEST_INTERVAL_MS = 500L        // 최소 0.5초 간격
        private const val SMALLEST_DISPLACEMENT_M = 5f      // 최소 5m 이동 시 업데이트

        /**
         * 서비스 시작
         * Start service
         */
        fun startService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * 서비스 중지
         * Stop service
         */
        fun stopService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            context.stopService(intent)
        }
    }

    // FusedLocationProviderClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // LocationCallback
    private lateinit var locationCallback: LocationCallback

    // NotificationManager
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()

        // FusedLocationProviderClient 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // NotificationManager 초기화
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Notification Channel 생성 (Android 8.0+)
        createNotificationChannel()

        // LocationCallback 초기화
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                // 가장 정확한 위치 선택
                val location = locationResult.locations.firstOrNull {
                    it.accuracy >= 0
                } ?: return

                // FDLocationManager로 위치 전달
                FDLocationManager.getInstance(applicationContext).addLocation(location)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Foreground Service 시작
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // 위치 업데이트 시작
        startLocationUpdates()

        // 서비스가 종료되어도 재시작
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        // 위치 업데이트 중지
        stopLocationUpdates()
    }

    override fun onBind(intent: Intent?): IBinder? {
        // 바인딩 불필요 (startService 방식 사용)
        return null
    }

    /**
     * Notification Channel 생성 (Android 8.0+)
     * Create notification channel
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "낚시 일기 위치 추적 알림"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Notification 생성
     * Create notification
     */
    private fun createNotification(): Notification {
        // 앱 메인 화면으로 이동하는 PendingIntent
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("낚시 일기")
            .setContentText("위치 추적 중...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)  // 사용자가 스와이프로 제거 불가
            .build()
    }

    /**
     * 위치 업데이트 시작
     * Start location updates
     */
    private fun startLocationUpdates() {
        // 권한 체크
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // LocationRequest 생성
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            UPDATE_INTERVAL_MS
        ).apply {
            setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
            setMinUpdateDistanceMeters(SMALLEST_DISPLACEMENT_M)
            setWaitForAccurateLocation(true)
        }.build()

        // 위치 업데이트 요청
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    /**
     * 위치 업데이트 중지
     * Stop location updates
     */
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
