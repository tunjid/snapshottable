plugins {
    alias(libs.plugins.kotlin.jvm)
    id("kotlin-jvm-convention")
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
    }
}

dependencies {
    compileOnly(libs.kotlin.compiler)
    compileOnly(libs.kotlin.stdlib)
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}
