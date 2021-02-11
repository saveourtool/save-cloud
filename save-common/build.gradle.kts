plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = Versions.jdk
            }
        }
    }
    js(IR).browser()
}
