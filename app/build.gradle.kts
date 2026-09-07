plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.paydaytracker.app"
    compileSdk = 35
    defaultConfig { applicationId = "com.paydaytracker.app"; minSdk = 26; targetSdk = 35; versionCode = 17; versionName = "2.1.1" }
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
