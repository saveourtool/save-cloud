package com.saveourtool.save.frontend.components.views.vuln.utils

val schemaVersionDescription = """
    The `schema_version` field is used to indicate which version of the **COSV** schema
    a particular vulnerability was exported with.
    This can help consumer applications decide how to import the data for
    their own systems and offer some protection against future breaking changes.
    The value should be a string matching the **COSV** schema version, which follows
    the [SemVer 2.0.0](https://semver.org/) format, with no leading “v” prefix. If no value is specified,
    it should be assumed to be `1.0.0`, matching version `1.0` of the **COSV** schema.
""".trimIndent()

