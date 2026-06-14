plugins {
    alias(libs.plugins.kotlin.jvm)
    id("compiler-compat-convention")
}

dependencies {
    implementation(project(":compiler-compat:k2420_dev_3583"))
}
