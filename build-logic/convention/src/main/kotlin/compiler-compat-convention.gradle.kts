import ext.configureKotlinJvm

/*
 *    Copyright 2024 Adetunji Dahunsi
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

plugins {
    // Note: Don't use alias() inside precompiled script plugins directly unless
    // configured via version catalogs in the build-logic build.
    // Usually, you apply the exact ID:
    id("org.jetbrains.kotlin.jvm")
}

configureKotlinJvm()

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("src/resources"))
    }
}

dependencies {
    val kotlinVersion = providers.fileContents(layout.projectDirectory.file("version.txt"))
        .asText.map { it.trim() }

    compileOnly(kotlinVersion.map { "org.jetbrains.kotlin:kotlin-compiler:$it" })

    compileOnly("org.jetbrains.kotlin:kotlin-stdlib")
    api(project(":compiler-compat"))
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}
