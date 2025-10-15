plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services") // 🔥 Cần cho Firebase
}

android {
    namespace = "com.example.finalproject"
    compileSdk = 36 // ⚠️ Dùng SDK chính thức hiện có

    defaultConfig {
        applicationId = "com.example.finalproject"
        minSdk = 24 // đủ để chạy Firebase ổn định
        targetSdk = 34
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
        // 🔹 Vì bạn dùng Java nên giữ version 11 (hoặc 17 nếu đã nâng cấp)
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // AndroidX cơ bản
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.coordinatorlayout)

    // Firebase BOM (bom = Bill of Materials -> quản lý version đồng bộ)
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))

    // Firebase modules bạn cần
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore") // 🔥 thêm dòng này, vì bạn dùng Java

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // BCrypt (mã hóa mật khẩu)
    implementation("org.mindrot:jbcrypt:0.4")

    // Unit tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Glide - thư viện load ảnh từ URL
    implementation("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")

    // Thư viện slide ảnh tự động
    implementation("com.github.denzcoskun:ImageSlideshow:0.1.2")
    implementation("com.google.firebase:firebase-firestore:25.1.1")

    // Firebase Storage
    implementation("com.google.firebase:firebase-storage:21.0.0")
}
