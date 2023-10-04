package com.saveourtool.save.frontend.components.views.vuln.utils

val schemaVersionDescr = """
    The `schema_version` field is used to indicate which version of the **COSV** schema
    a particular vulnerability was exported with.
    This can help consumer applications decide how to import the data for
    their own systems and offer some protection against future breaking changes.
    The value should be a string matching the **COSV** schema version, which follows
    the [SemVer 2.0.0](https://semver.org/) format, with no leading “v” prefix. If no value is specified,
    it should be assumed to be `1.0.0`, matching version `1.0` of the **COSV** schema.
""".trimIndent()

val idModifiedDescr = """
    The id field is a unique identifier for the vulnerability entry.
    It is a string of the format `<DB>-<ENTRYID>`, where `DB` names the database and `ENTRYID`
    is in the format used by the database.
    For example: `“OSV-2020-111”`, `“CVE-2021-3114”`, or `“GHSA-vp9c-fpxx-744v”`.
""".trimIndent()

val publishedDescr = """
    The `published` field gives the time the entry should be considered to have been published,
    as an RFC3339-formatted time stamp in UTC (ending in “Z”).
""".trimIndent()

val withdrawnDescr = """
    The withdrawn field gives the time the entry should be considered to have been withdrawn,
    as an RFC3339-formatted timestamp in UTC (ending in “Z”).
    If the field is missing, then the entry has not been withdrawn.
    Any rationale for why the vulnerability has been withdrawn should go into the summary text.
""".trimIndent()

val aliasesDescr = """
    The aliases field gives a list of IDs of the same vulnerability in other databases,
    in the form of the id field.
    This allows one database to claim that its own entry describes
    the same vulnerability as one or more entries in other databases.

    Aliases should be considered symmetric and transitive.
""".trimIndent()

val cweIdsDescr = """
    The public defect enumeration ID's corresponding to the vulnerability type
""".trimIndent()

val cweNamesDescr = """
    The public defect enumeration name corresponding to the vulnerability type
""".trimIndent()

val timelineDescr = """
    The life cycle of the vulnerability itself,
    this should be distinguished from “published” or “withdrawn”
    which describes time points of this vulnerability entry not the vulnerability itself.
    
    timeline[].type

    The type of time point on the timeline, including but not limited to: “introduced”, “found”, “fixed”, “disclosed”; type of time point

    timeline[].value

    The value of the point in time, in line with the timestamp of UTC an RFC3339-formatted time stamp in UTC (ending in “Z”), e.g. “2023-03-13T16:43Z”
""".trimIndent()

