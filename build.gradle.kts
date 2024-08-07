// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        classpath(libs.gradle)
        classpath("com.github.dcendents:android-maven-gradle-plugin:2.1")
    }
}

plugins {
    id("com.android.library") version "8.4.2" apply false
    id("com.android.application") version "8.4.2" apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
}
