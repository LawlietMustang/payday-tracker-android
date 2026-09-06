plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.paydaytracker.app"
    compileSdk = 35
    defaultConfig { applicationId = "com.paydaytracker.app"; minSdk = 26; targetSdk = 35; versionCode = 11; versionName = "1.8.1" }
    buildTypes { release { isMinifyEnabled = false; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
