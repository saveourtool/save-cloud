import com.saveourtool.save.buildutils.*

@Suppress("DSL_SCOPE_VIOLATION", "RUN_IN_SCRIPT")  // https://github.com/gradle/gradle/issues/22797
plugins {
    id("com.saveourtool.save.buildutils.versioning-configuration")
    id("com.saveourtool.save.buildutils.code-quality-convention")
    id("com.saveourtool.save.buildutils.publishing-configuration")
    alias(libs.plugins.talaiot.base)
    java
}

val profile = properties.getOrDefault("save.profile", "dev") as String

registerLiquibaseTask(profile)

//        register("sandbox") {
//            arguments = mapOf(
//                "changeLogFile" to "save-sandbox/db/db.changelog-sandbox.xml",
//                "liquibaseSchemaName" to "save_sandbox",
//                "defaultSchemaName" to "save_sandbox",
//            ) +
//                    getSandboxDatabaseCredentials(profile).toLiquibaseArguments() +
//                    commonArguments
//        }
//        register("demo") {
//            arguments = mapOf(
//                "changeLogFile" to "save-demo/db/db.changelog-demo.xml",
//                "liquibaseSchemaName" to "save_demo",
//                "defaultSchemaName" to "save_demo",
//            ) +
//                    getDemoDatabaseCredentials(profile).toLiquibaseArguments() +
//                    commonArguments
//        }

talaiot {
    metrics {
        // disabling due to problems with OSHI on some platforms
        performanceMetrics = false
        environmentMetrics = false
    }
    publishers {
        timelinePublisher = true
    }
}

allprojects {
    configurations.all {
        // if SNAPSHOT dependencies are used, refresh them periodically
        resolutionStrategy.cacheDynamicVersionsFor(10, TimeUnit.MINUTES)
        resolutionStrategy.cacheChangingModulesFor(10, TimeUnit.MINUTES)
    }
}

createStackDeployTask(profile)
