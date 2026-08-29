plugins {
    id("com.android.application")
}

android {
    namespace = "com.ricardo.notasvozlocal"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ricardo.notasvozlocal"
        minSdk = 26
        targetSdk = 34
        versionCode = 8
        versionName = "0.8-ideas-icon"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    implementation("com.alphacephei:vosk-android:0.3.47")
}
