plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {

    sourceSets {
        named("main") {
            res.srcDirs("./res")
            manifest.srcFile("./AndroidManifest.xml")
        kotlin.srcDirs("./kotlin")
    }
    }
    namespace = "com.aisuluaiva.talkbackutils"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.android.talkback"
        minSdk = 30
        targetSdk = 33
        versionCode = 1
        versionName = "0.1 BETA"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {

    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core:1.12.0")
    implementation("org.json:json:20210307")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.0.0")
}
