plugins {
    kotlin("jvm") version "2.2.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0"
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    compileOnly(files("/home/zen/Android/Sdk/platforms/android-36/android.jar"))
    compileOnly("androidx.compose.runtime:runtime:1.7.8")
    compileOnly("androidx.compose.ui:ui:1.7.8")
    compileOnly("androidx.compose.ui:ui-unit:1.7.8")
    compileOnly("androidx.compose.ui:ui-graphics:1.7.8")
    compileOnly("androidx.compose.foundation:foundation-layout:1.7.8")
    compileOnly("androidx.compose.material3:material3:1.3.1")
}

kotlin {
    jvmToolchain(17)
}

tasks.register<Jar>("frameworkJar") {
    archiveFileName.set("framework.jar")
    destinationDirectory.set(file("../../system_libs"))
    from(sourceSets.main.get().output) {
        include("android/**")
    }
}

tasks.register<Jar>("settingsLibJar") {
    archiveFileName.set("SettingsLib.jar")
    destinationDirectory.set(file("../../system_libs"))
    from(sourceSets.main.get().output) {
        include("com/android/settingslib/DeviceInfoUtils*")
        include("com/android/settingslib/utils/**")
    }
}

tasks.register<Jar>("spaLibJar") {
    archiveFileName.set("SpaLib.jar")
    destinationDirectory.set(file("../../system_libs"))
    from(sourceSets.main.get().output) {
        include("com/android/settingslib/spa/**")
        include("META-INF/**")
    }
}

tasks.register("generateAllStubs") {
    dependsOn("frameworkJar", "settingsLibJar", "spaLibJar")
}
