pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://redirector.kotlinlang.org/maven/bootstrap")
        maven("https://redirector.kotlinlang.org/maven/dev/")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        maven("https://redirector.kotlinlang.org/maven/bootstrap")
        maven("https://redirector.kotlinlang.org/maven/dev/")
    }
}

rootProject.name = "snapshottable"

include("compiler-plugin")
include("gradle-plugin")
include("plugin-annotations")
include(":compiler-compat")

// Dynamically include all per-version compat modules (directories under compiler-compat/
// whose names start with "k" and contain a version.txt).
rootProject.projectDir.resolve("compiler-compat").listFiles()?.forEach { dir ->
    if (dir.isDirectory && dir.name.startsWith("k") && java.io.File(dir, "version.txt").exists()) {
        include(":compiler-compat:${dir.name}")
    }
}
