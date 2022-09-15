package com.saveourtool.save.domain

import kotlinx.serialization.Serializable

/**
 * @property projectCoordinates file belongs to the project with such coordinates
 * @property name name of file
 * @property uploadedMillis version of file
 */
@Serializable
data class FileKey(
    val projectCoordinates: ProjectCoordinates,
    val name: String,
    val uploadedMillis: Long,
) {
    companion object {
        const val FIELD_DELIMITER = ":"
        const val OBJECT_DELIMITER = ";"
    }
}

/**
 * @return formatted string
 */
fun List<FileKey>.format(): String = joinToString(FileKey.OBJECT_DELIMITER) {
    listOf(it.projectCoordinates.organizationName, it.projectCoordinates.projectName, it.name, it.uploadedMillis)
        .joinToString(FileKey.FIELD_DELIMITER)
}

/**
 * @return formatted string for storing on Execution
 */
fun List<FileKey>.formatForExecution(): String {
    if (isEmpty()) {
        return ""
    }
    require(this.map { it.projectCoordinates }.distinct().count() == 1) {
        "All FileKey on a single execution should have same ProjectCoordinates"
    }
    return this.joinToString(FileKey.OBJECT_DELIMITER) {
        listOf(it.name, it.uploadedMillis)
            .joinToString(FileKey.FIELD_DELIMITER)
    }
}

/**
 * @return list of [FileKey]s parsed from provided string
 */
@Suppress(
    "WRONG_OVERLOADING_FUNCTION_ARGUMENTS",
    "DestructuringDeclarationWithTooManyEntries"
)
fun String.toFileKeyList(): List<FileKey> = if (isEmpty()) {
    emptyList()
} else {
    split(FileKey.OBJECT_DELIMITER)
        .map { elementStr ->
            val (organizationName, projectName, name, uploadedMillis) = elementStr.split(FileKey.FIELD_DELIMITER)
            FileKey(
                projectCoordinates = ProjectCoordinates(organizationName, projectName),
                name = name,
                uploadedMillis = uploadedMillis.toLong()
            )
        }
}

/**
 * @param projectCoordinates
 * @return list of [FileKey]s parsed from provided string using provided [ProjectCoordinates]
 */
@Suppress(
    "DestructuringDeclarationWithTooManyEntries"
)
fun String.toFileKeyList(projectCoordinates: ProjectCoordinates): List<FileKey> = if (isEmpty()) {
    emptyList()
} else {
    split(FileKey.OBJECT_DELIMITER)
        .map { elementStr ->
            val (name, uploadedMillis) = elementStr.split(FileKey.FIELD_DELIMITER)
            FileKey(
                projectCoordinates = projectCoordinates,
                name = name,
                uploadedMillis = uploadedMillis.toLong()
            )
        }
}
