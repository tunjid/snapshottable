import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.gradle.java.test.fixtures)
    alias(libs.plugins.gradle.idea)
    alias(libs.plugins.shadow)
    id("kotlin-jvm-convention")
    id("publishing-library-convention")
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }
    testFixtures {
        java.setSrcDirs(listOf("test-fixtures"))
    }
    test {
        java.setSrcDirs(listOf("test", "test-gen"))
        resources.setSrcDirs(listOf("testData"))
    }
}

idea {
    module.generatedSourceDirs.add(projectDir.resolve("test-gen"))
}

val annotationsRuntimeClasspath: Configuration by configurations.creating { isTransitive = false }
val composeRuntimeClasspath: Configuration by configurations.creating { isTransitive = true }

val testArtifacts: Configuration by configurations.creating

val embedded by configurations.dependencyScope("embedded")
val embeddedClasspath by configurations.resolvable("embeddedClasspath") {
    extendsFrom(embedded)
}
configurations.named("compileOnly").configure { extendsFrom(embedded) }
configurations.named("testImplementation").configure { extendsFrom(embedded) }

dependencies {
    compileOnly(libs.kotlin.compiler)

    add(embedded.name, project(":compiler-compat"))
    rootProject.projectDir.resolve("compiler-compat").listFiles()?.forEach { dir ->
        if (dir.isDirectory && dir.name.startsWith("k") && File(dir, "version.txt").exists()) {
            add(embedded.name, project(":compiler-compat:${dir.name}"))
        }
    }

    testFixturesApi(libs.kotlin.test.junit5)
    testFixturesApi(libs.kotlin.test.framework)
    testFixturesApi(libs.kotlin.compiler)

    annotationsRuntimeClasspath(project(":plugin-annotations"))
    composeRuntimeClasspath(libs.compose.multiplatform.runtime)

    // Dependencies required to run the internal test framework.
    testArtifacts(libs.kotlin.stdlib)
    testArtifacts(libs.kotlin.stdlib.jdk8)
    testArtifacts(libs.kotlin.reflect)
    testArtifacts(libs.kotlin.test)
    testArtifacts(libs.kotlin.script.runtime)
    testArtifacts(libs.kotlin.annotations.jvm)
}

buildConfig {
    useKotlinOutput {
        internalVisibility = true
    }

    packageName(group.toString())
    buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${rootProject.group}\"")
}

tasks.test {
    dependsOn(annotationsRuntimeClasspath)

    useJUnitPlatform()
    workingDir = rootDir

    systemProperty("annotationsRuntime.classpath", annotationsRuntimeClasspath.asPath)
    systemProperty("composeRuntime.classpath", composeRuntimeClasspath.asPath)

    // Properties required to run the internal test framework.
    setLibraryProperty("org.jetbrains.kotlin.test.kotlin-stdlib", "kotlin-stdlib")
    setLibraryProperty("org.jetbrains.kotlin.test.kotlin-stdlib-jdk8", "kotlin-stdlib-jdk8")
    setLibraryProperty("org.jetbrains.kotlin.test.kotlin-reflect", "kotlin-reflect")
    setLibraryProperty("org.jetbrains.kotlin.test.kotlin-test", "kotlin-test")
    setLibraryProperty("org.jetbrains.kotlin.test.kotlin-script-runtime", "kotlin-script-runtime")
    setLibraryProperty("org.jetbrains.kotlin.test.kotlin-annotations-jvm", "kotlin-annotations-jvm")

    systemProperty("idea.ignore.disabled.plugins", "true")
    systemProperty("idea.home.path", rootDir)
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
        optIn.add("org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI")
    }
}

val generateTests by tasks.registering(JavaExec::class) {
    inputs.dir(layout.projectDirectory.dir("testData"))
        .withPropertyName("testData")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(layout.projectDirectory.dir("test-gen"))
        .withPropertyName("generatedTests")

    classpath = sourceSets.testFixtures.get().runtimeClasspath
    mainClass.set("com.tunjid.snapshottable.GenerateTestsKt")
    workingDir = rootDir
}

tasks.compileTestKotlin {
    dependsOn(generateTests)
}

fun Test.setLibraryProperty(propName: String, jarName: String) {
    val path = testArtifacts.files
        .find { """$jarName-\d.*""".toRegex().matches(it.name) }
        ?.absolutePath
        ?: return
    systemProperty(propName, path)
}

// Shadow jar: bundles compiler-compat + all per-version compat implementations into a single
// fat jar with merged META-INF/services. ServiceLoader picks the right impl at plugin runtime
// based on the hosting compiler's META-INF/compiler.version.
tasks.named<Jar>("jar") {
    enabled = false
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    configurations = listOf(embeddedClasspath)
    dependencies {
        // Shadow defaults to including every dep; we only want the compat modules, not
        // the Kotlin compiler (those come from the host at runtime).
        exclude(dependency("org.jetbrains.kotlin:.*"))
        exclude(dependency("org.jetbrains:annotations:.*"))
    }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()
}

// Replace the disabled regular jar with the shadow jar in the outgoing variants so
// downstream consumers (and mavenPublish) receive the fat jar.
for (configurationName in arrayOf("apiElements", "runtimeElements")) {
    configurations.named(configurationName) {
        outgoing.artifacts.clear()
        outgoing.artifact(shadowJar)
    }
}

// Shadow's default shadowJar task depends on `jar` being enabled; since we disabled it,
// make `assemble` still produce something.
tasks.named("assemble") { dependsOn(shadowJar) }
