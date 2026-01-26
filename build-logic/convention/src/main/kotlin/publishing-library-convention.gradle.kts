plugins {
    id("com.vanniktech.maven.publish")
    id("pl.allegro.tech.build.axion-release")
    id("org.jetbrains.dokka")
    signing
}

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

allprojects {
    group = "com.tunjid.snapshottable"
    version = "0.1.0-SNAPSHOT"
//    version = scmVersion.version

    task("printProjectVersion") {
        doLast {
            println(">> " + project.name + " version is " + version)
        }
    }
}

val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class) {
    dokkaSourceSets {
        try {
            named("iosTest") {
                suppress.set(true)
            }
        } catch (e: Exception) {
        }
    }
}

val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaHtml.outputDirectory)
}

mavenPublishing {
    pom {
        name.set(project.name)
        description.set("A Kotlin compiler plugin for generating mutable classes backed by Compose Snapshot state")
        url.set("https://github.com/tunjid/snapshottable")
        licenses {
            license {
                name.set("Apache License 2.0")
                url.set("https://github.com/tunjid/snapshottable/blob/main/LICENSE")
            }
        }
        developers {
            developer {
                id.set("tunjid")
                name.set("Adetunji Dahunsi")
                email.set("tjdah100@gmail.com")
            }
        }
        scm {
            connection.set("scm:git:github.com/tunjid/snapshottable.git")
            developerConnection.set("scm:git:ssh://github.com/tunjid/snapshottable.git")
            url.set("https://github.com/tunjid/snapshottable/tree/main")
        }
    }

    val username = project.providers.gradleProperty("mavenCentralUsername")
    val password = project.providers.gradleProperty("mavenCentralPassword")

    if (username.isPresent && password.isPresent) {
        publishToMavenCentral()
        signAllPublications()
    }
}

signing {
    val signingKey = project.providers
        .gradleProperty("signingInMemoryKey")
        .orNull

    val signingPassword = project.providers
        .gradleProperty("signingInMemoryKeyPassword")
        .orNull

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
}

val signingTasks = tasks.withType<Sign>()
tasks.withType<AbstractPublishToMaven>().configureEach {
    dependsOn(signingTasks)
}
