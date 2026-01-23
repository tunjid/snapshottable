import org.jetbrains.kotlin.gradle.plugin.NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm")
}

group = "org.jetbrains.kotlin.compiler.plugin.template"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

dependencies {
    add(PLUGIN_CLASSPATH_CONFIGURATION_NAME, project(":compiler-plugin"))
    add(NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME, project(":compiler-plugin"))
    compileOnly(project(":plugin-annotations"))

    implementation("androidx.compose.runtime:runtime:1.10.0")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}