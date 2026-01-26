package com.tunjid.snapshottable

import com.tunjid.snapshottable.BuildConfig.ANNOTATIONS_LIBRARY_COORDINATES
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.Companion.COMMON_MAIN_SOURCE_SET_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

@Suppress("unused") // Used via reflection.
class SnapshottableGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        target.extensions.create(
            /* name = */ "SnapshottablePlugin",
            /* type = */ SnapshottableGradleExtension::class.java,
        )

        val annotationDependency = target.providers.provider {
            target.dependencyFactory.create(ANNOTATIONS_LIBRARY_COORDINATES)
        }

        target.pluginManager.withPlugin(MULTIPLATFORM_PLUGIN) {
            val kotlin = target.extensions.getByName(KotlinExtension) as KotlinSourceSetContainer
            val commonMainSourceSet = kotlin.sourceSets.getByName(COMMON_MAIN_SOURCE_SET_NAME)

            target.configurations.named(commonMainSourceSet.implementationConfigurationName).configure {

                it.dependencies.addLater(annotationDependency)
            }
        }

        target.pluginManager.withPlugin(JVM_PLUGIN) {
            val sourceSets = target.extensions.getByName(SourceSetsExtension) as SourceSetContainer
            sourceSets.configureEach { sourceSet ->
                target.configurations.named(sourceSet.implementationConfigurationName).configure {
                    it.dependencies.addLater(annotationDependency)
                }
            }
        }
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = BuildConfig.KOTLIN_PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = BuildConfig.KOTLIN_PLUGIN_GROUP,
        artifactId = BuildConfig.KOTLIN_PLUGIN_NAME,
        version = BuildConfig.KOTLIN_PLUGIN_VERSION,
    )

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>,
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        return project.provider {
            project.extensions.getByType(SnapshottableGradleExtension::class.java)
            emptyList()
        }
    }
}

private const val KotlinExtension = "kotlin"
private const val SourceSetsExtension = "sourceSets"

private const val MULTIPLATFORM_PLUGIN = "org.jetbrains.kotlin.multiplatform"
private const val JVM_PLUGIN = "org.jetbrains.kotlin.jvm"
