import org.cqfn.save.buildutils.configureDetekt
import org.cqfn.save.buildutils.configureDiktat
import org.cqfn.save.buildutils.createDetektTask
import org.cqfn.save.buildutils.createDiktatTask
import org.cqfn.save.buildutils.installGitHooks

plugins {
    kotlin("jvm") version "1.4.21" apply false
    id("com.github.ben-manes.versions") version "0.36.0"
}

allprojects {
    repositories {
        jcenter()
        mavenCentral()
    }
    configureDiktat()
    configureDetekt()
}

createDiktatTask()
createDetektTask()
installGitHooks()