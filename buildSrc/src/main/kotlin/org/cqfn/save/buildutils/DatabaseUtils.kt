@file:Suppress("FILE_NAME_MATCH_CLASS", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package org.cqfn.save.buildutils

import org.gradle.api.Project
import java.io.File

/**
 * @property databaseUrl database URL in the same format as used by jdbc
 * @property username username to connect
 * @property password password to connect
 */
data class DatabaseCredentials(
    val databaseUrl: String,
    val username: String,
    val password: String
)

/**
 * @param profile a profile to get credentials for
 * @return an instance of [DatabaseCredentials] for [profile]
 */
fun Project.getDatabaseCredentials(profile: String): DatabaseCredentials {
    val props = java.util.Properties()
    file("save-backend/src/main/resources/application-$profile.properties").apply {
        props.load(inputStream())
    }

    if (File("${System.getenv()["HOME"]}/secrets").exists()) {
        file("${System.getenv()["HOME"]}/secrets").apply { props.load(inputStream()) }
    }

    val databaseUrl: String
    val username: String
    val password: String

    if (profile == "prod") {
        databaseUrl = props.getProperty("spring.datasource.url")
        username = props.getProperty("username")
        password = props.getProperty("password")
    } else {
        databaseUrl = props.getProperty("datasource.dev.url")
        username = props.getProperty("spring.datasource.username")
        password = props.getProperty("spring.datasource.password")
    }

    return DatabaseCredentials(databaseUrl, username, password)
}
