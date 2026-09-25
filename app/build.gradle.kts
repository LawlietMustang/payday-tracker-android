plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.paydaytracker.app"
    compileSdk = 35
    defaultConfig { applicationId = "com.paydaytracker.app"; minSdk = 26; targetSdk = 35; versionCode = 39; versionName = "2.4.10"; testInstrumentationRunner = "com.paydaytracker.app.DeviceSmoke" }
    signingConfigs {
        create("permanent") {
            System.getenv("PAYDAY_KEYSTORE_PATH")?.let { storeFile = file(it) }
            storePassword = System.getenv("PAYDAY_KEYSTORE_PASSWORD")
            keyAlias = "payday"
            keyPassword = System.getenv("PAYDAY_KEYSTORE_PASSWORD")
        }
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug"; versionNameSuffix = "-debug" }
        release {
            signingConfig = signingConfigs.getByName("permanent")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
}
