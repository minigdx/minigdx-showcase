plugins {
    id("com.github.minigdx.jvm") version "DEV-SNAPSHOT"
    id("com.github.minigdx.js") version "DEV-SNAPSHOT"
    id("com.github.minigdx.common") version "DEV-SNAPSHOT"
}

group = "com.github.minigdx"
version = "1.0-SNAPSHOT"

minigdx {
    jvm.mainClass.set("showcase.Main")
    version.set("DEV-SNAPSHOT")
}
