package com.kip2.apkinstaller.service

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.application.runReadAction
import com.kip2.apkinstaller.util.callMethod
import com.kip2.apkinstaller.util.callMethodSafe
import java.io.File

data class SigningConfig(
    val name: String,
    val storeFile: String?,
    val storePassword: String?,
    val keyAlias: String?,
    val keyPassword: String?,
    val moduleName: String = "unknown"
)

/**
 * Professional service to extract signing information directly from the Gradle External System model.
 * robustly handling DataKey lookups without hard dependencies.
 */
class GradleSigningService(private val project: Project) {

    private val log: Logger = Logger.getInstance(GradleSigningService::class.java)

    fun getSigningConfigs(module: Module): List<SigningConfig> {
        return runReadAction {
            try {
                val gradleSystemId = ProjectSystemId("GRADLE")
                val modulePath = ExternalSystemApiUtil.getExternalProjectPath(module) ?: return@runReadAction emptyList()
                val projectsData = ProjectDataManager.getInstance().getExternalProjectsData(project, gradleSystemId)
                log.info("Found ${projectsData.size} external projects data for $gradleSystemId")

                for (projectData in projectsData) {
                    val projectStructure = projectData.externalProjectStructure ?: continue
                    val androidModels = findAllAndroidModels(projectStructure)

                    for (node in androidModels) {
                        val model = node.data ?: continue
                        // Try to find the module name from the node hierarchy if possible, otherwise use the passed module.name
                        val effectiveModuleName = findModuleName(node) ?: module.name
                        return@runReadAction extractSigningConfigsFromModel(model, effectiveModuleName)
                    }
                }
                emptyList()
            } catch (e: Exception) {
                log.warn("Failed to get signing configs for module ${module.name}: ${e.message}")
                emptyList()
            }
        }
    }

    fun getAllSigningConfigs(): List<SigningConfig> {
        return runReadAction {
            val configs = mutableListOf<SigningConfig>()
            val gradleSystemId = ProjectSystemId("GRADLE")
            val projectsData = ProjectDataManager.getInstance().getExternalProjectsData(project, gradleSystemId)

            for (projectData in projectsData) {
                val projectStructure = projectData.externalProjectStructure ?: continue
                val androidModels = findAllAndroidModels(projectStructure)

                for (node in androidModels) {
                    val model = node.data ?: continue
                    val moduleName = findModuleName(node) ?: "unknown"
                    configs.addAll(extractSigningConfigsFromModel(model, moduleName))
                }
            }
            configs
        }
    }

    private fun findModuleName(node: DataNode<*>): String? {
        var current: DataNode<*>? = node
        while (current != null) {
            if (current.key.dataType.contains("ModuleData")) {
                 val data = current.data ?: return null
                 val name = data.callMethodSafe("getModuleName") as? String
                 if (!name.isNullOrEmpty()) return name
                 return data.callMethodSafe("getExternalName") as? String
            }
            current = current.parent
        }
        return null
    }

    private fun findAllAndroidModels(rootNode: DataNode<*>): List<DataNode<*>> {
        val result = mutableListOf<DataNode<*>>()

        fun traverse(node: DataNode<*>) {
            val keyString = node.key.toString()
            val dataClassName = node.data?.javaClass?.name ?: ""
            log.info("Traversing DataNode: key=$keyString, dataClass=$dataClassName")
            if (keyString.contains("AndroidProject") || 
                keyString.contains("AndroidModuleModel") || 
                keyString.contains("GradleAndroidModelData") ||
                dataClassName.contains("AndroidProject") ||
                dataClassName.contains("AndroidModuleModel") ||
                dataClassName.contains("GradleAndroidModelData")) {
                result.add(node)
            }
            for (child in node.children) {
                traverse(child)
            }
        }
        traverse(rootNode)
        return result
    }
    private fun extractSigningConfigsFromModel(model: Any, moduleName: String): List<SigningConfig> = runCatching {
        // model might be GradleAndroidModelData or AndroidModuleModel
        // Both usually have getAndroidProject()
        val androidProject = model.callMethod("getAndroidProject")

        val projectType = runCatching {
            androidProject.callMethod("getProjectType").callMethod("name")
        }.getOrNull()
        
        if (projectType == "PROJECT_TYPE_LIBRARY") {
            return emptyList()
        }
        
        val configs = androidProject.callMethod("getSigningConfigs") as Collection<*>
        configs.filterNotNull().map { config ->
            val name = config.callMethodSafe("getName") as? String ?: ""
            val storeFile = config.callMethodSafe("getStoreFile") as? File?
            val storePassword = config.callMethodSafe("getStorePassword") as? String?
            val keyAlias = config.callMethodSafe("getKeyAlias") as? String?
            val keyPassword = config.callMethodSafe("getKeyPassword") as? String?
            
            SigningConfig(name, storeFile?.absolutePath, storePassword, keyAlias, keyPassword, moduleName)
        }
    }.getOrElse { emptyList() }

    fun findModuleForFile(filePath: String): Module? {
        return runReadAction {
            val file = com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByPath(filePath) ?: return@runReadAction null
            log.info("Finding module for file: $filePath")
            com.intellij.openapi.module.ModuleUtilCore.findModuleForFile(file, project)
        }
    }
}
