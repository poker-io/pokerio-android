// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.0.0" apply false
    id("com.android.library") version "8.0.0" apply false
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false
}

buildscript {
    repositories {
        // Firebase
        google()
        mavenCentral()
    }
    dependencies {
        // Firebase
        classpath("com.google.gms:google-services:4.3.15")
    }
}