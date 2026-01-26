plugins {
    alias(libs.plugins.axionRelease) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.binary.compatibility.validator) apply false
    alias(libs.plugins.jetbrains.dokka) apply false
    alias(libs.plugins.buildconfig) apply false
}

allprojects {
    group = "com.tunjid.snapshottable"
}
