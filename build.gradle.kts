import org.cqfn.save.buildutils.configureDetekt
import org.cqfn.save.buildutils.configureDiktat
import org.cqfn.save.buildutils.installGitHooks

plugins {
    kotlin("jvm") version "1.4.21" apply false
}

allprojects {
    repositories {
        jcenter()
        mavenCentral()
    }
    configureDiktat()
    configureDetekt()
}

installGitHooks()