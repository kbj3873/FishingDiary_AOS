package com.onbada.seathermo.services

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
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.onbada.seathermo.R
import com.onbada.seathermo.managers.FDLocationManager
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
        private const val UPDATE_INTERVAL_MS = 2000L        // 2초마다 업데이트
        private const val FASTEST_INTERVAL_MS = 1000L       // 최소 1초 간격
        private const val SMALLEST_DISPLACEMENT_M = 2f      // 최소 2m 이동 시 업데이트

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

    // 위치 콜백 전용 백그라운드 스레드.
    // [개념] UI 스레드(MainLooper) 대신 별도 HandlerThread에서 위치 콜백을 처리합니다.
    //        iOS의 CLLocationManager가 별도 큐에서 콜백을 처리하는 것과 동일한 이유입니다.
    //        UI 스레드 부하 시 콜백 지연을 방지하여 위치 업데이트 누락을 줄입니다.
    private lateinit var locationHandlerThread: HandlerThread

    override fun onCreate() {
        super.onCreate()

        // FusedLocationProviderClient 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // NotificationManager 초기화
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Notification Channel 생성 (Android 8.0+)
        createNotificationChannel()

        // 위치 콜백 전용 백그라운드 스레드 시작
        locationHandlerThread = HandlerThread("LocationCallbackThread").also { it.start() }

        // LocationCallback 초기화
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                // 배치로 전달된 위치 중 가장 최신 유효 위치를 사용합니다.
                // [개념] Android FusedLocationProvider는 여러 위치를 배치로 전달할 수 있습니다.
                //        firstOrNull은 가장 오래된 위치를 선택하므로 lastOrNull이 올바릅니다.
                //        iOS didUpdateLocations는 호출당 1개씩 전달하므로 이 차이가 없습니다.
                val location = locationResult.locations.lastOrNull {
                    it.accuracy >= 0
                } ?: return

                android.util.Log.d("Location Debug", "Service received location: ${location.latitude}, ${location.longitude}, accuracy=${location.accuracy}")

                // FDLocationManager로 위치 전달
                FDLocationManager.getInstance(applicationContext).addLocation(location)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("Location Debug", "LocationTrackingService started")
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

        // 백그라운드 스레드 종료
        locationHandlerThread.quitSafely()

        // 앱 종료 등으로 서비스가 소멸될 때 미저장 위치 데이터를 저장합니다.
        // [개념] ForegroundService.onDestroy()는 앱 강제 종료 시에도 대부분 호출됩니다.
        //        FDLocationManager.stopTracking()이 여기서 호출되지 않으면
        //        locationList에 쌓인 마지막 데이터가 유실됩니다.
        FDLocationManager.getInstance(applicationContext).stopTracking()
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
            .setContentTitle("온바다 - 낚시 기록 중")
            .setContentText("현재 이동 경로를 안전하게 기록하고 있습니다.")
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
            // [개념] setWaitForAccurateLocation(true): GPS가 충분한 정확도를 확보한 후 첫 위치를 전달합니다.
            //        false이면 Wi-Fi/기지국 기반의 저정확도 위치를 즉시 전달하여 경로가 튀는 현상이 발생합니다.
            //        true로 설정하면 첫 위치 전달이 수 초 지연될 수 있으나, 이후 추적은 정상적으로 진행됩니다.
            //        해상·야외 환경에서는 GPS 확보가 빠르므로 실용적 지연이 거의 없습니다.
            setWaitForAccurateLocation(true)
        }.build()

        // 위치 업데이트 요청 (백그라운드 스레드 루퍼 사용)
        // [개념] locationHandlerThread.looper를 사용하면 UI 스레드 부하와 무관하게
        //        위치 콜백이 즉시 처리됩니다.
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            locationHandlerThread.looper
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
