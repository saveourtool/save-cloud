rootProject.name = "save-cloud"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

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

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
