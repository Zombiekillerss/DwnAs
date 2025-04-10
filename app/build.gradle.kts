plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.dwnas"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.dwnas"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters.add("x86")
            abiFilters.add("x86_64")
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
        }
        ksp{
            room{
                schemaDirectory("$projectDir/schemas")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        compose = true
        viewBinding = true
    }

    splits.abi {
        isEnable = true
        reset()
        include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
        isUniversalApk = true
    }
}

dependencies {
//    implementation(libs.androidx.media3.exoplayer)
//    implementation(libs.androidx.media3.exoplayer.dash)
//    implementation(libs.androidx.media3.ui)
//    implementation(libs.androidx.media3.database)
//    implementation(libs.androidx.media3.datasource)


    implementation(libs.library.v0172)
    implementation(libs.ffmpeg)
    //implementation(libs.prdownloader)
    //implementation("io.github.junkfood02.youtubedl-android:aria2c:0.17.2")
    implementation (libs.rangeseekbar.library)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.cardview)

    annotationProcessor(libs.androidx.room.compiler)
    implementation(libs.androidx.room.runtime)

    implementation(libs.okhttp)
    implementation (libs.jsoup)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.recyclerview)
    add("ksp", libs.androidx.room.compiler)
}
