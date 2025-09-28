// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("file:D:/Files/Gradle/repo") }
        maven { url = uri("file:D:/Files/Maven/apache-maven-3.6.1/repo") }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.2.2")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("file:D:/Files/Gradle/repo") }
        maven { url = uri("file:D:/Files/Maven/apache-maven-3.6.1/repo") }
    }
}