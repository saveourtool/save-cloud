package com.saveourtool.save.frontend.components.views.vuln.utils


val schemaVersion = "schema_version" to schemaVersionDescr

val idModified = "id, modified" to idModifiedDescr

val published = "published" to publishedDescr

val withdrawn = "withdrawn" to withdrawnDescr

val aliases = "aliases" to aliasesDescr

val cweIds = "cwe_ids" to cweIdsDescr

val cweNames = "cwe_names" to cweNamesDescr

val timeline = "timeline" to timelineDescr


val cosvFieldsDescriptionsList = listOf(
        schemaVersion,
        idModified,
        withdrawn,
        aliases,
        cweIds,
        cweNames,
        timeline
)