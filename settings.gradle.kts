rootProject.name = "minigdx-showcase"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        maven {
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        }.mavenContent {
            includeVersionByRegex("com.github.minigdx.(.*)", "(.*)", "LATEST-SNAPSHOT")
            includeVersionByRegex("com.github.minigdx", "(.*)", "LATEST-SNAPSHOT")
        }
        mavenLocal()
    }
}

plugins {
    id("com.github.minigdx.settings") version ("DEV-SNAPSHOT")
}

include("common")
include("jvm")
include("web")
