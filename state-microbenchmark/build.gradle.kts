plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.snapshottable)
    // NB: the androidx.benchmark Gradle plugin is NOT applied — 1.4.1 expects AGP's
    // legacy TestedExtension, removed in AGP 9. Like :macrobenchmark, we configure the
    // runner/build type manually and depend on the benchmark runtime directly; the
    // self-instrumenting library androidTest makes IsolationActivity + permission
    // grants resolve against the test APK's own manifest.
}

android {
    namespace = "com.tunjid.snapshottable.microbenchmark"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
        testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"
    }

    // Microbenchmarks must run against non-debuggable code for trustworthy numbers.
    // This is a self-instrumenting library module (test APK == app under test), so the
    // benchmark harness (IsolationActivity, permission grants) works, unlike an
    // app-module androidTest.
    testBuildType = "release"
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Needed by the snapshottable-generated SnapshotMutable code (mutableStateOf, ...).
    implementation(compose.runtime)

    androidTestImplementation(libs.androidx.benchmark.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.junit)
}
