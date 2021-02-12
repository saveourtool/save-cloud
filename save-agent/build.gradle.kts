import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem

plugins {
    kotlin("multiplatform")
}

kotlin {
    val os = getCurrentOperatingSystem()
    // Create a target for the host platform.
    val hostTarget = when {
        os.isLinux -> linuxX64()
        os.isWindows -> mingwX64()  // you'll need to install msys2 and run `pacman -S mingw-w64-x86_64-curl` to have libcurl for ktor-client
        else -> throw GradleException("Host OS '${os.name}' is not supported in Kotlin/Native $project.")
    }

    configure(listOf(hostTarget)) {
        binaries.executable()
    }
    sourceSets {
        val nativeMain by creating {
            dependencies {
                implementation(project(":save-common"))
                implementation("io.ktor:ktor-client-core:${Versions.ktor}")
                implementation("io.ktor:ktor-client-curl:${Versions.ktor}")
            }
        }
        getByName("${hostTarget.name}Main").dependsOn(nativeMain)
        val nativeTest by creating
        getByName("${hostTarget.name}Test").dependsOn(nativeTest)
    }
}
