// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
}

// Keep disposable build output outside OneDrive, which can lock generated files
// while Gradle replaces them. Source files remain in the project directory.
if (System.getProperty("os.name").startsWith("Windows") &&
    rootDir.toPath().any { it.toString().startsWith("OneDrive", ignoreCase = true) }
) {
    val projectKey = rootDir.absolutePath.hashCode().toUInt().toString(16)
    val outputRoot = java.io.File(gradle.gradleUserHomeDir, "yaadride-builds/$projectKey")
    allprojects {
        val modulePath = if (path == ":") "root" else path.drop(1).replace(':', '/')
        layout.buildDirectory.set(java.io.File(outputRoot, modulePath))
    }
}