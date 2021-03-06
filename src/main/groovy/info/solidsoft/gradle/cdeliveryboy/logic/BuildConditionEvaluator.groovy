package info.solidsoft.gradle.cdeliveryboy.logic

import groovy.transform.CompileStatic
import info.solidsoft.gradle.cdeliveryboy.infra.PropertyReader
import info.solidsoft.gradle.cdeliveryboy.logic.config.CDeliveryBoyPluginConfig
import info.solidsoft.gradle.cdeliveryboy.logic.config.CiVariablesConfig
import info.solidsoft.gradle.cdeliveryboy.logic.config.ProjectConfig

@CompileStatic
class BuildConditionEvaluator {

    private final CiVariablesConfig ciConfig
    private final CDeliveryBoyPluginConfig pluginConfig
    private final PropertyReader environmentVariableReader
    private final ProjectConfig projectConfig

    BuildConditionEvaluator(CiVariablesConfig ciConfig, CDeliveryBoyPluginConfig pluginConfig, PropertyReader environmentVariableReader,
                            ProjectConfig projectConfig) {
        this.ciConfig = ciConfig
        this.pluginConfig = pluginConfig
        this.environmentVariableReader = environmentVariableReader
        this.projectConfig = projectConfig
    }

    boolean isInReleaseBranch() {
        return environmentVariableReader.findByName(ciConfig.isPrName) == "false" &&
                environmentVariableReader.findByName(ciConfig.branchNameName) == pluginConfig.git.releaseBranch
    }

    boolean isReleaseTriggered() {
        if (environmentVariableReader.findByName(pluginConfig.trigger.skipReleaseVariableName) == "true") {
            return false
        }
        return !pluginConfig.trigger.releaseOnDemand ||
                environmentVariableReader.findByName(ciConfig.commitMessageName)?.contains(pluginConfig.trigger.onDemandReleaseTriggerCommand)
    }

    boolean isSnapshotVersion() {
        return projectConfig.version.endsWith('-SNAPSHOT') && !isInDryRunModeWithForcedNonSnapshotVersion()
    }

    private boolean isInDryRunModeWithForcedNonSnapshotVersion() {
        return (pluginConfig.dryRun || projectConfig.globalDryRun) && pluginConfig.dryRunForceNonSnapshotVersion
    }

    String getReleaseConditionsAsString() {
        //TODO: Maybe on lifecycle display only not satisfied conditions (how to do it in a clearly way)?
        //TODO: Move information about being a snapshot version here
        return "Branch name: ${environmentVariableReader.findByName(ciConfig.branchNameName)} (configured: ${pluginConfig.git.releaseBranch}), " +
                "is PR: ${environmentVariableReader.findByName(ciConfig.isPrName)}, " +
                "release on demand: ${pluginConfig.trigger.releaseOnDemand}, " +
                "on demand trigger command: '${environmentVariableReader.findByName(ciConfig.commitMessageName)}' " +
                "(configured: '${pluginConfig.trigger.onDemandReleaseTriggerCommand}', " +
                "is SNAPSHOT: '${isSnapshotVersion()}')"
    }
}
