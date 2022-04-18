rootProject.name = "save-cloud"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots") {
            content {
                includeGroup("org.cqfn.save")
            }
        }
        maven {
            url = uri("https://maven.pkg.github.com/analysis-dev/sarif4k")
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

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
