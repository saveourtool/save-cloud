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

include("save-backend")
include("save-orchestrator")
include("save-frontend")
include("save-cloud-common")
include("save-agent")
include("save-preprocessor")
