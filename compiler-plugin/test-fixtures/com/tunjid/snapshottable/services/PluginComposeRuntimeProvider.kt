package com.tunjid.snapshottable.services

import java.io.File
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices

private val composeRuntimeClassPath =
    System.getProperty("composeRuntime.classpath")?.split(File.pathSeparator)?.map(::File)
        ?: error("Unable to get a valid classpath from 'composeRuntime.classpath' property")

fun TestConfigurationBuilder.configureComposeRuntime() {
    useConfigurators(::ComposeRuntimeProvider)
    useCustomRuntimeClasspathProviders(::ComposeRuntimeClasspathProvider)
}

private class ComposeRuntimeProvider(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        configuration.addJvmClasspathRoots(composeRuntimeClassPath)
    }
}

private class ComposeRuntimeClasspathProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule) = composeRuntimeClassPath
}
