import org.cqfn.save.buildutils.configureDetekt
import org.cqfn.save.buildutils.configureDiktat
import org.cqfn.save.buildutils.configureVersioning
import org.cqfn.save.buildutils.createDetektTask
import org.cqfn.save.buildutils.createDiktatTask
import org.cqfn.save.buildutils.createStackDeployTask
import org.cqfn.save.buildutils.installGitHooks

plugins {
    kotlin("jvm") version Versions.kotlin apply false
    id("com.github.ben-manes.versions") version "0.38.0"
    id("com.cdsap.talaiot.plugin.base") version "1.4.1"
}

talaiot {
    publishers {
        timelinePublisher = true
    }
}
allprojects {
    repositories {
        jcenter()
        mavenCentral()
    }
    configureDiktat()
    configureDetekt()
}

createStackDeployTask()
configureVersioning()
createDiktatTask()
createDetektTask()
installGitHooks()