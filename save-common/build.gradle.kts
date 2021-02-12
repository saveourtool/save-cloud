plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js(IR).browser()

    // setup native compilation
    val os = org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem()
    val hostTarget = when {
        os.isLinux -> linuxX64()
        os.isWindows -> mingwX64()
        else -> throw GradleException("Host OS '${os.name}' is not supported in Kotlin/Native $project.")
    }
}
