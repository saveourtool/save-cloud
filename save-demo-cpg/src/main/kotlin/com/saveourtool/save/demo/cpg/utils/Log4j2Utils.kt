/**
 * This class contains util methods for Log4j 2
 */

package com.saveourtool.save.demo.cpg.utils;

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Appender
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.WriterAppender
import org.apache.logging.log4j.core.config.AppenderRef
import org.apache.logging.log4j.core.config.Configuration
import org.apache.logging.log4j.core.config.LoggerConfig
import java.io.StringWriter
import kotlin.random.Random


typealias ResultWithLogs<R> = Pair<R, List<String>>

/**
 * @param loggerName
 * @param function
 * @return result of [function] with logs captured for [loggerName]
 */
fun <R> runAndCaptureLogs(
    loggerName: String,
    function: () -> R,
): ResultWithLogs<R> {
    val appenderName = "capture-logs-${Random.nextInt(100, 999)}"
    val writer = StringWriter()
    val context = LogManager.getContext(false)
        .let { it as LoggerContext }
    val config = context.configuration

    val appender = config.createAppender(appenderName, writer)

    val alreadyExists = config.loggers.contains(loggerName)
    val loggerConfig = if (alreadyExists) {
        config.getLoggerConfig(loggerName)
    } else {
        config.createLogger(loggerName)
    }
    loggerConfig.addAppender(appender, null, null)
    context.updateLoggers()

    try {
        val result: R = function()
        return result to writer.toString().lines()
    } finally {
        loggerConfig.removeAppender(appenderName)
        if (!alreadyExists) {
            config.removeLogger(loggerName)
        }
        config.appenders
        context.updateLoggers()
    }
}

private fun Configuration.createAppender(
    appenderName: String,
    writer: StringWriter,
): WriterAppender = WriterAppender.newBuilder()
        .setConfiguration(this)
        .setTarget(writer)
        .setName(appenderName)
        .build()
        .also { it.start() }
        .also { this.addAppender(it) }

private fun Appender.toRefs(): Array<AppenderRef> = arrayOf(AppenderRef.createAppenderRef(this.name, null, null))

private fun Configuration.createLogger(
    loggerName: String,
): LoggerConfig = LoggerConfig.newBuilder()
    .withConfig(this)
    .withLoggerName(loggerName)
    .withLevel(Level.ALL)
    .withAdditivity(false)
    .build()
    .also { this.addLogger(loggerName, it) }