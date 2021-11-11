package configurations

import common.buildToolGradleParameters
import common.customGradle
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.GradleBuildStep
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import model.CIBuildModel
import model.Stage

class Gradleception(model: CIBuildModel, stage: Stage) : BaseGradleBuildType(stage = stage, init = {
    id("${model.projectId}_Gradleception")
    name = "Gradleception - Java8 Linux"
    description = "Builds Gradle with the version of Gradle which is currently under development (twice)"

    features {
        publishBuildStatusToGithub(model)
    }

    failureConditions {
        javaCrash = false
    }

    params {
        // Override the default commit id so the build steps produce reproducible distribution
        param("env.BUILD_COMMIT_ID", "HEAD")
    }

    // So that the build steps produces reproducible distribution
    val magicTimestamp = "20210102030405+0000"
    val buildScanTagForType = buildScanTag("Gradleception")
    val defaultParameters = (buildToolGradleParameters() + listOf(buildScanTagForType) + "-Porg.gradle.java.installations.auto-download=false").joinToString(separator = " ")

    applyDefaults(model, this, ":distributions-full:install", notQuick = true, extraParameters = "-Pgradle_installPath=dogfood-first -PignoreIncomingBuildReceipt=true -PbuildTimestamp=$magicTimestamp $buildScanTagForType", extraSteps = {
        script {
            name = "CALCULATE_DISTRIBUTION_MD5"
            scriptContent = """
                #!/bin/sh
                set -x
                MD5=`find %teamcity.build.checkoutDir%/dogfood-first -type f | sort | xargs md5sum | md5sum | awk '{ print ${'$'}1 }'`
                echo "##teamcity[setParameter name='env.ORG_GRADLE_PROJECT_versionQualifier' value='gradleception-${'$'}MD5']"
            """.trimIndent()
        }

        localGradle {
            name = "BUILD_WITH_BUILT_GRADLE"
            tasks = "clean :distributions-full:install"
            gradleHome = "%teamcity.build.checkoutDir%/dogfood-first"
            gradleParams = "-Pgradle_installPath=dogfood-second -PignoreIncomingBuildReceipt=true -PbuildTimestamp=$magicTimestamp $defaultParameters"
        }
        localGradle {
            name = "QUICKCHECK_WITH_GRADLE_BUILT_BY_GRADLE"
            tasks = "clean sanityCheck test"
            gradleHome = "%teamcity.build.checkoutDir%/dogfood-second"
            gradleParams = defaultParameters
        }
    })
})

fun BuildSteps.localGradle(init: GradleBuildStep.() -> Unit): GradleBuildStep =
    customGradle(init) {
        param("ui.gradleRunner.gradle.wrapper.useWrapper", "false")
        buildFile = ""
    }
