plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.paydaytracker.app"
    compileSdk = 36
    buildFeatures {
        buildConfig = true
        compose = true
    }
    if (providers.gradleProperty("resourceAudit").isPresent) {
        lint { checkOnly += "UnusedResources"; warningsAsErrors = true }
    }
    defaultConfig { applicationId = "com.paydaytracker.app"; minSdk = 26; targetSdk = 36; versionCode = 40; versionName = "2.4.11"; testInstrumentationRunner = "com.paydaytracker.app.DeviceSmoke" }
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

    // Room persistence
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Compose BOM & UI
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("junit:junit:4.13.2")
}

tasks.withType<Test> {
    useJUnit()
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
