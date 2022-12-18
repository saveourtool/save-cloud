package com.saveourtool.save.buildutils

plugins {
    id("com.saveourtool.save.buildutils.detekt-convention-configuration")
    id("com.saveourtool.save.buildutils.diktat-convention-configuration")
    id("com.saveourtool.save.buildutils.spotless-convention-configuration")
}

configureJacoco()
