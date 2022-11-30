/**
 * This class contains util methods for NEO4J
 */

package com.saveourtool.save.demo.cpg.utils

import arrow.core.Either
import arrow.core.computations.ResultEffect.bind
import arrow.core.continuations.either
import arrow.core.continuations.result
import arrow.core.getOrHandle
import arrow.core.left
import arrow.core.right
import org.neo4j.driver.exceptions.AuthenticationException
import org.neo4j.ogm.config.Configuration
import org.neo4j.ogm.exception.ConnectionException
import org.neo4j.ogm.session.Session
import org.neo4j.ogm.session.SessionFactory
import java.lang.IllegalArgumentException


typealias SessionWithFactory = Pair<Session, SessionFactory>

fun tryConnect(
    uri: String,
    username: String,
    password: String,
    packageName: String
): Either<ConnectionException, SessionWithFactory> = try {
    val configuration =
        Configuration.Builder()
            .uri(uri)
            .autoIndex("none")
            .credentials(username, password)
            .verifyConnection(true)
            .build()
    val sessionFactory = SessionFactory(configuration, packageName)
    val session = requireNotNull(sessionFactory.openSession()) {
        "Failed to open session"
    }
    (session to sessionFactory).right()
} catch (ex: ConnectionException) {
    ex.left()
} catch (ex: AuthenticationException) {
    throw IllegalArgumentException("Unable to connect to $uri, wrong username/password of the database", ex)
}

fun <R> SessionWithFactory.use(action: (Session) -> R): R {
    try {
        return action(first)
    } finally {
        first.clear()
        second.close()
    }
}

fun <R, E : Throwable> retry(
    maxRetry: Int,
    supplier: () -> Either<E, R>
): R {
    return generateSequence(supplier) { previousResult ->
        if (previousResult.isLeft()) {
            // wait
            supplier()
        } else {
            either {

            }
            previousResult
        }
    }
        .filterIndexed { attempt, result ->
            result.isRight() || attempt >= maxRetry
        }
        .first()
        .getOrHandle {
            throw it
        }
}