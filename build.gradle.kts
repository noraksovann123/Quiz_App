buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.0") // Use appropriate version
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0") // Use appropriate version
        classpath("com.google.gms:google-services:4.3.15") // For Firebase
    }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules
plugins {
    id("com.android.application") version "8.1.0" apply false
    id("com.android.library") version "8.1.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}