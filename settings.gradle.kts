rootProject.name = "save-cloud"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots") {
            content {
                includeGroup("org.cqfn.save")
            }
        }
    }
}

val isIncludeSaveApi: String? = System.getProperty("includeSaveApi")

include("api-gateway")
include("save-backend")
include("save-orchestrator")
include("save-frontend")
include("save-cloud-common")
include("save-agent")
include("save-preprocessor")
include("test-utils")
if (isIncludeSaveApi != null) {
    include("save-api")
}
includeBuild("sarif4k")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
