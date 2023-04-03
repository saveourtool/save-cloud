/**
 * This file contains util methods for logback
 */

package com.saveourtool.save.demo.cpg.utils

import arrow.core.Either
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.OutputStreamAppender
import org.apache.commons.io.output.StringBuilderWriter
import org.apache.commons.io.output.WriterOutputStream
import org.slf4j.LoggerFactory

object LogbackCapturer {
    private const val APPENDER_NAME = "capture-logs-appender"
    private val writer = StringBuilderWriter()
    private val context by lazy {
        LoggerFactory.getILoggerFactory()
            .let { it as LoggerContext }
    }
    private val appender: Appender<ILoggingEvent> by lazy {
        val ple = PatternLayoutEncoder()
            .apply {
                this.pattern = "%d{HH:mm:ss.SSS} [%thread] %level %logger{36} - %msg %ex{5}%n"
                this.context = LogbackCapturer.context
            }
            .also { it.start() }

        OutputStreamAppender<ILoggingEvent>()
            .apply {
                this.name = APPENDER_NAME
                this.encoder = ple
                this.outputStream = WriterOutputStream(writer, Charsets.UTF_8)
                this.context = LogbackCapturer.context
                this.isImmediateFlush = true
            }
            .also { it.start() }
    }

    /**
     * @param loggerName
     * @param function
     * @return result of [function] with logs captured for [loggerName]
     */
    operator fun <R> invoke(loggerName: String, function: () -> R): ResultWithLogs<R> {
        val loggerConfig = context.getLogger(loggerName)
        loggerConfig.addAppender(appender)
        val result: Either<Throwable, R> = Either.catch { function() }
        val logs = writer.toString().lines()
        loggerConfig.detachAppender(appender)
        writer.builder.clear()
        return ResultWithLogs(result, logs)
    }
}
