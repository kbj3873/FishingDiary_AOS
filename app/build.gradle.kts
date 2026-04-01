import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.onbada.seathermo"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.onbada.seathermo"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // local.properties에서 API 키 읽어오기
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }

        // BuildConfig에 API 키 추가
        buildConfigField("String", "RISA_API_KEY", "\"${localProperties.getProperty("RISA_API_KEY", "")}\"")
        buildConfigField("String", "API_BASE_URL", "\"${localProperties.getProperty("API_BASE_URL", "https://seathermo.com")}\"")
        buildConfigField("String", "ONBADA_BASE_URL", "\"${localProperties.getProperty("ONBADA_BASE_URL", "")}\"")
        // Map API Keys
        buildConfigField("String", "GOOGLE_MAPS_KEY", "\"${localProperties.getProperty("GOOGLE_MAPS_KEY", "")}\"")
        buildConfigField("String", "KAKAO_APP_KEY", "\"${localProperties.getProperty("KAKAO_APP_KEY", "")}\"")

        // Manifest에서 사용할 수 있도록 변수 주입
        manifestPlaceholders["GOOGLE_MAPS_KEY"] = localProperties.getProperty("GOOGLE_MAPS_KEY", "")
        manifestPlaceholders["KAKAO_APP_KEY"] = localProperties.getProperty("KAKAO_APP_KEY", "")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 릴리즈 빌드 시 ProGuard/R8 난독화 매핑 파일을 Crashlytics에 자동 업로드합니다.
            // isMinifyEnabled = true로 변경 시 크래시 스택 트레이스가 올바르게 복원됩니다.
            firebaseCrashlytics {
                mappingFileUploadEnabled = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // UI Components
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2") // For viewModelScope

    // JSON serialization/deserialization
    implementation("com.google.code.gson:gson:2.10.1")

    // HTTP client
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Room (로컬 데이터베이스)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Coil (이미지 로딩 라이브러리 - 로컬 파일 경로에서 썸네일 로드에 사용)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // KakaoMap SDK
    implementation("com.kakao.maps.open:android:2.13.0")
    // Google Maps SDK
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // Firebase BOM (버전 통합 관리)
    // [개념] BOM(Bill of Materials)을 사용하면 Firebase 라이브러리 버전을 개별 지정 없이
    //        BOM 버전 하나로 일괄 관리할 수 있습니다.
    val firebaseBom = platform("com.google.firebase:firebase-bom:33.11.0")
    implementation(firebaseBom)
    implementation("com.google.firebase:firebase-analytics")      // 사용자 행동 분석
    implementation("com.google.firebase:firebase-crashlytics")    // 크래시 리포트

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}