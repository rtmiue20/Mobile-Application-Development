plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "vn.edu.ueh.rtmiue20.project"
    compileSdk = 37

    defaultConfig {
        applicationId = "vn.edu.ueh.rtmiue20.project"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    // Firebase & Google Services
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.play.services.auth)
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // THÊM THƯ VIỆN BẮT THÔNG BÁO FCM Ở ĐÂY:
    implementation("com.google.firebase:firebase-messaging")

    // Image Loading
    implementation(libs.glide)

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // Thêm thư viện OkHttp để lát nữa viết code gửi thông báo đi cho dễ
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
