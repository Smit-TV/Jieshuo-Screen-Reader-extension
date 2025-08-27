plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.android.talkback"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.android.talkback" // for GBoard
        minSdk = 28
        targetSdk = 34
        versionCode = 60162623 // the last version of talkback 16.0 Gboard will check it every time when want to show keyboard
        versionName = "2025.08.27"
    }

    sourceSets {
        named("main") {
            res.srcDirs("./res")
            manifest.srcFile("./AndroidManifest.xml")
            kotlin.srcDirs("./kotlin")
        }
    }

    lintOptions {
        disable("MissingTranslation")
    }

    buildFeatures {
        androidResources {
            generateLocaleConfig = true
        }
    }

    signingConfigs {
        create("release") {
            keyAlias = "mykeyalias"
            // Рекомендуется не хранить пароли в явном виде в build.gradle;
            // вынесите их в gradle.properties и читайте через project.property(...)
            keyPassword = "password123@" as String
            storeFile = file("./myreleasekey.jks")
            storePassword = "password123@" as String
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs["release"]
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    // если не используются специфичные buildFeatures, можно убрать блок,
    // иначе перечислите нужные, например:
    // buildFeatures {
    //     viewBinding = true
    // }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.preference:preference:1.2.0")
}
