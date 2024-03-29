plugins {
    id("com.android.application")
    kotlin("android") version "1.8.10"
    kotlin("plugin.serialization") version "1.8.10"
    id("org.jlleitschuh.gradle.ktlint") version "11.3.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.0-RC3"

    // Firebase
    id("com.google.gms.google-services")
}

fun versionCode(): Int {
    val secondsSinceEpoch = System.currentTimeMillis() / 1000
    // This will fail eventually, but well… It's the best we have
    return secondsSinceEpoch.toInt()
}

android {
    namespace = "com.pokerio.app"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.pokerio.app"
        minSdk = 24
        targetSdk = 33
        versionCode = versionCode()
        versionName = "0.5-alpha"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("debug") {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
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
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }
}

tasks.register<Copy>("installGitHooks") {
    val source = File(rootProject.rootDir, "scripts/pre-commit")
    val destination = File(rootProject.rootDir, ".git/hooks")

    from(source)
    into(destination)
}

tasks.getByName("build").dependsOn("installGitHooks")

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2023.01.00")

    implementation("androidx.core:core-ktx:1.10.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.2")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.3.1")
    testImplementation("org.mockito:mockito-core:5.2.0")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Jetpack Compose
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.7.1")
    implementation("androidx.navigation:navigation-compose:2.5.3")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.30.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:31.2.3"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
}
