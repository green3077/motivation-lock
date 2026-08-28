import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// release 서명 정보는 keystore.properties.example을 참고해 로컬에 keystore.properties를
// 만들면(git에는 커밋하지 않음) 적용된다. 없으면 release도 debug 서명으로 빌드된다(사이드로드
// 개인 앱이라 지금 단계에선 이 정도로 충분 - 필요해지면 그때 release 키를 만든다).
val keystorePropertiesFile = rootProject.file("keystore.properties")
val hasReleaseSigning = keystorePropertiesFile.exists()
val keystoreProperties = Properties().apply {
    if (hasReleaseSigning) load(keystorePropertiesFile.inputStream())
}

android {
    namespace = "com.green3077.motivationlock"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.green3077.motivationlock"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }

    signingConfigs {
        getByName("debug") {
            // 저장소에 커밋된 고정 디버그 키. 어느 PC에서 빌드하든 서명이 같아야
            // 기기에 이미 설치된 앱 위에 새 APK를 덮어 설치(업데이트)할 수 있다.
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    testImplementation("junit:junit:4.13.2")
}
