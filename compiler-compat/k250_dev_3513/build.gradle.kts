plugins {
    alias(libs.plugins.kotlin.jvm)
    id("compiler-compat-convention")
}

dependencies {
    implementation(project(":compiler-compat:k250_dev_498"))
}
