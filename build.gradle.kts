plugins {
    id("com.github.minigdx.jvm") version "LATEST-SNAPSHOT"
    id("com.github.minigdx.js") version "LATEST-SNAPSHOT"
    id("com.github.minigdx.common") version "LATEST-SNAPSHOT"
}

group = "com.github.minigdx"
version = "1.0-SNAPSHOT"

minigdx {
    jvm.mainClass.set("demo.MainKt")
    version.set("DEV-SNAPSHOT")
}
