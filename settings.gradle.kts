rootProject.name = "save-cloud"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("com.gradle.enterprise") version "3.12.2"
}

includeBuild("gradle/plugins")
include("api-gateway")
include("save-backend")
include("save-orchestrator-common")
include("save-orchestrator")
include("save-frontend")
include("save-cloud-common")
include("save-agent")
include("save-preprocessor")
include("test-utils")
include("save-api")
include("save-api-cli")
include("save-sandbox")
include("authentication-service")
include("save-demo")
include("save-demo-cpg")
include("test-analysis-core")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

gradleEnterprise {
    if (System.getenv("CI") != null) {
        buildScan {
            publishAlways()
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
        }
    }
}
