pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "compiler-plugin-template"

include("compiler-plugin")
include("gradle-plugin")
include("plugin-annotations")

include("playground")
