plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.example.farmsteadcalendar"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.farmsteadcalendar"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    val room_version = "2.6.1"

    // ROOM
    implementation("androidx.room:room-runtime:$room_version")

    // ЗАМІСТЬ annotationProcessor ПИШЕМО kapt:
    kapt("androidx.room:room-compiler:$room_version")

    implementation("com.applandeo:material-calendar-view:1.9.0")
    implementation("com.google.android.material:material:1.11.0") // Також переконайтеся, що ця бібліотека підключена
    // Решта твоїх залежностей...
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")

    // TESTS
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}