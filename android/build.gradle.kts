// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.service) apply false
}

buildscript{
    repositories{
        google()
        mavenCentral()
    }

    dependencies {
        // Dependency added to ensure compatibility with recent versions.
        classpath(libs.javapoet)
    }
}