rootProject.name = "save-cloud"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots") {
            content {
                includeGroup("com.saveourtool.save")
            }
        }
        maven {
            url = uri("https://maven.pkg.github.com/saveourtool/sarif4k")
            val gprUser: String? by settings
            val gprKey: String? by settings
            credentials {
                username = gprUser
                password = gprKey
            }
            content {
                includeGroup("io.github.detekt.sarif4k")
            }
        }
    }
}

include("api-gateway")
include("save-backend")
include("save-orchestrator")
include("save-frontend")
include("save-cloud-common")
include("save-agent")
include("save-preprocessor")
include("test-utils")
include("save-cloud-charts")
include("save-api")
include("save-api-cli")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
