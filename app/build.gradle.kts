plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.paydaytracker.app"
    compileSdk = 35
    buildFeatures { buildConfig = true }
    if (providers.gradleProperty("resourceAudit").isPresent) {
        lint { checkOnly += "UnusedResources"; warningsAsErrors = true }
    }
    defaultConfig { applicationId = "com.paydaytracker.app"; minSdk = 26; targetSdk = 35; versionCode = 41; versionName = "2.4.12"; testInstrumentationRunner = if (providers.gradleProperty("upgradeTest").isPresent) "com.paydaytracker.app.UpgradeSmoke" else "com.paydaytracker.app.DeviceSmoke" }
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

// Generate packaged assets; never rewrite developer source during a build.
val webVersion = android.defaultConfig.versionName!!
val prepareWebAssets by tasks.registering(Sync::class) {
    from("src/main/assets")
    into(layout.buildDirectory.dir("generated/webAssets"))
    inputs.property("webVersion", webVersion)
    filesMatching("**/index.html") {
        filter { line: String ->
            line.replace(Regex("""((?:src|href)=")([^"?]+\.(?:js|css))(?:\?v=[^"]*)?(")""")) {
                "${it.groupValues[1]}${it.groupValues[2]}?v=$webVersion${it.groupValues[3]}"
            }
        }
    }
}
android.sourceSets.getByName("main").assets.setSrcDirs(listOf(layout.buildDirectory.dir("generated/webAssets")))
tasks.named("preBuild").configure { dependsOn(prepareWebAssets) }
