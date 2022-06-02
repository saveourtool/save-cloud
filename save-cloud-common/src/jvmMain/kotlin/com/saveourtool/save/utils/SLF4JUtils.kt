package com.saveourtool.save.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun Logger.debug(msg: () -> String) {
    if (this.isDebugEnabled) {
        debug(msg())
    }
}

inline fun Logger.info(msg: () -> String) {
    if (this.isInfoEnabled) {
        info(msg())
    }
}

inline fun Logger.warn(msg: () -> String) {
    if (this.isWarnEnabled) {
        warn(msg())
    }
}

inline fun Logger.error(msg: () -> String) {
    if (this.isErrorEnabled) {
        error(msg())
    }
}

inline fun <reified T> getLogger(): Logger = LoggerFactory.getLogger(T::class.java)