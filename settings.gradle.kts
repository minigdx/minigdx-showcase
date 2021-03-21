rootProject.name = "minigdx-showcase"

// includeBuild("../3d-skeleton")


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
        jcenter()
    }
}
