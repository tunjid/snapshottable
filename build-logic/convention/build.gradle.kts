import org.jetbrains.kotlin.gradle.dsl.JvmTarget


plugins {
    `kotlin-dsl`
}

group = "com.tunjid.snapshottable.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.axionRelease.gradlePlugin)
    implementation(libs.dokka.gradlePlugin)
    implementation(libs.maven.publish.gradlePlugin)
}
