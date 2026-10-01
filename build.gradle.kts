plugins {
    alias(libs.plugins.axionRelease)
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.binary.compatibility.validator) apply false
    alias(libs.plugins.jetbrains.dokka) apply false
    alias(libs.plugins.buildconfig) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose.multiplatform) apply false
}

// Axion is applied only here, at the repository root. When applied to a subproject it
// derives the version from the last commit touching that subproject's directory, so
// modules without changes since the last tag would publish at a different version.
scmVersion {
    tag {
        // Use an empty string for prefix
        prefix.set("")
    }
    repository {
        pushTagsOnly.set(true)
    }
    providers.gradleProperty("library.releaseBranch")
        .orNull
        ?.let { releaseBranch ->
            when {
                releaseBranch.contains("bugfix/") -> versionIncrementer("incrementPatch")
                releaseBranch.contains("feature/") -> versionIncrementer("incrementMinor")
                releaseBranch.contains("release/") -> versionIncrementer("incrementMajor")
                else -> throw IllegalArgumentException("Unknown release type")
            }
        }
}

val projectVersion = scmVersion.version

allprojects {
    group = "com.tunjid.snapshottable"
    version = projectVersion

    tasks.register("printProjectVersion") {
        val name = project.name
        val version = project.version
        doLast {
            println(">> $name version is $version")
        }
    }
}
