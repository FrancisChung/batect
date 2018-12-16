/*
   Copyright 2017-2018 Charles Korn.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package batect.config.io

import batect.config.BuildImage
import batect.config.Container
import batect.config.EnvironmentVariableExpression
import batect.config.HealthCheckConfig
import batect.config.ImageSource
import batect.config.PortMapping
import batect.config.PullImage
import batect.config.RunAsCurrentUserConfig
import batect.config.VolumeMount
import batect.config.io.deserializers.DependencySetDeserializer
import batect.config.io.deserializers.EnvironmentDeserializer
import batect.os.Command
import batect.os.PathResolutionResult
import batect.os.PathResolver
import batect.os.PathType
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

data class ContainerFromFile(
    val buildDirectory: String? = null,
    @JsonProperty("image") val imageName: String? = null,
    val command: Command? = null,
    @JsonDeserialize(using = EnvironmentDeserializer::class) val environment: Map<String, EnvironmentVariableExpression> = emptyMap(),
    val workingDirectory: String? = null,
    @JsonProperty("volumes") val volumeMounts: Set<VolumeMount> = emptySet(),
    @JsonProperty("ports") val portMappings: Set<PortMapping> = emptySet(),
    @JsonDeserialize(using = DependencySetDeserializer::class) val dependencies: Set<String> = emptySet(),
    @JsonProperty("health_check") val healthCheckConfig: HealthCheckConfig = HealthCheckConfig(),
    @JsonProperty("run_as_current_user") val runAsCurrentUserConfig: RunAsCurrentUserConfig = RunAsCurrentUserConfig()
) {

    fun toContainer(name: String, pathResolver: PathResolver): Container {
        val imageSource = resolveImageSource(name, pathResolver)

        val resolvedVolumeMounts = volumeMounts.map {
            resolveVolumeMount(it, name, pathResolver)
        }.toSet()

        if (!runAsCurrentUserConfig.enabled && runAsCurrentUserConfig.homeDirectory != null) {
            throw ConfigurationException("Container '$name' is invalid: running as the current user has not been enabled, but a home directory for that user has been provided.")
        }

        if (runAsCurrentUserConfig.enabled && runAsCurrentUserConfig.homeDirectory == null) {
            throw ConfigurationException("Container '$name' is invalid: running as the current user has been enabled, but a home directory for that user has not been provided.")
        }

        return Container(name, imageSource, command, environment, workingDirectory, resolvedVolumeMounts, portMappings, dependencies, healthCheckConfig, runAsCurrentUserConfig)
    }

    private fun resolveImageSource(containerName: String, pathResolver: PathResolver): ImageSource {
        if (buildDirectory == null && imageName == null) {
            throw ConfigurationException("Container '$containerName' is invalid: either build_directory or image must be specified.")
        }

        if (buildDirectory != null && imageName != null) {
            throw ConfigurationException("Container '$containerName' is invalid: only one of build_directory or image can be specified, but both have been provided.")
        }

        if (buildDirectory != null) {
            return BuildImage(resolveBuildDirectory(containerName, pathResolver, buildDirectory))
        } else {
            return PullImage(imageName!!)
        }
    }

    private fun resolveBuildDirectory(containerName: String, pathResolver: PathResolver, buildDirectory: String): String {
        when (val result = pathResolver.resolve(buildDirectory)) {
            is PathResolutionResult.Resolved -> when (result.pathType) {
                PathType.Directory -> return result.absolutePath.toString()
                PathType.DoesNotExist -> throw ConfigurationException("Build directory '$buildDirectory' (resolved to '${result.absolutePath}') for container '$containerName' does not exist.")
                else -> throw ConfigurationException("Build directory '$buildDirectory' (resolved to '${result.absolutePath}') for container '$containerName' is not a directory.")
            }
            is PathResolutionResult.InvalidPath -> throw ConfigurationException("Build directory '$buildDirectory' for container '$containerName' is not a valid path.")
        }
    }

    private fun resolveVolumeMount(volumeMount: VolumeMount, containerName: String, pathResolver: PathResolver): VolumeMount {
        val resolvedLocalPath = when (val result = pathResolver.resolve(volumeMount.localPath)) {
            is PathResolutionResult.Resolved -> result.absolutePath.toString()
            is PathResolutionResult.InvalidPath -> throw ConfigurationException("Local path '${volumeMount.localPath}' for volume mount in container '$containerName' is not a valid path.")
        }

        return VolumeMount(resolvedLocalPath, volumeMount.containerPath, volumeMount.options)
    }
}