package com.saveourtool.save.frontend.components.views.vuln.utils


val schemaVersion = "schema_version" to schemaVersionDescr

val idModified = "id, modified" to idModifiedDescr

val published = "published" to publishedDescr

val withdrawn = "withdrawn" to withdrawnDescr

val aliases = "aliases" to aliasesDescr

val cweIds = "cwe_ids" to cweIdsDescr

val cweNames = "cwe_names" to cweNamesDescr

val timeline = "timeline" to timelineDescr

val timelineType = "type" to timelineTypeDescr

val timelineValue = "valuy" to timelineValueDescr

val related = "related" to relatedDescr

val summaryDetails = "summary, details" to summaryDetailsDescr

val severity = "severity" to severityDescr

val severityType = "type" to severityTypeDescr

val severityScore = "score" to severityScoreDescr

val severityLevel = "level" to severityLevelDescr

val severityScoreNum = "score_num" to severityScoreNumDescr

val affected = "affected" to affectedDescr

val packageField = "package" to packageDescr

val packageEcosystem = "ecosystem" to packageEcosystemDescr

val packageName = "name" to packageNameDescr

val packagePurl = "purl" to packagePurlDescr

val packageLanguage = "language" to packageLanguageDescr

val packageRepository = "repository" to packageRepositoryDescr

val packageIntroducedCommits = "introduced_commits" to packageIntroducedCommitsDescr

val packageFixedCommits = "fixed_commits" to packageFixedCommitsDescr

val packageHomePage = "home_page" to packageHomePageDescr

val packageEdition = "edition" to packageEditionDescr

val affectedVersions = "affectedVersions" to affectedVersionsDescr

val ranges = "ranges" to rangesDescr

val rangesType = "type" to rangesTypeDescr

val rangesRepo = "repo" to rangesRepoDescr

val rangesEvents = "events" to rangesEventsDescr

val rangesDatabaseSpecific = "database_specific" to rangesDatabaseSpecificDescr

val affectedEcosystemSpecific = "ecosystem_specific" to affectedEcosystemSpecificDescr

val affectedDatabaseSpecific = "database_specific" to affectedDatabaseSpecificDescr

val patchesDetail = "patches_detail" to patchesDetailDescr

val patchesPatchUrl = "patch_url" to patchesPatchUrlDescr

val patchesIssueUrl = "issue_url" to patchesIssueUrlDescr

val patchesMainLanguage = "main_language" to patchesMainLanguageDescr

val patchesAuthor = "author" to patchesAuthorDescr

val patchesCommiter = "commiter" to patchesCommiterDescr

val patchesBranches = "branches" to patchesBranchesDescr

val patchesTags = "tags" to patchesTagsDescr


val cosvFieldsDescriptionsList = listOf(
        schemaVersion,
        idModified,
        published,
        withdrawn,
        aliases,
        cweIds,
        cweNames,
        timeline,
        timelineType,
        timelineValue,
        related,
        summaryDetails,
        severity,
        severityType,
        severityScore,
        severityLevel,
        severityScoreNum,
        packageField,
        packageEcosystem,
        packageName,
        packagePurl,
        packageLanguage,
        packageRepository,
        packageIntroducedCommits,
        packageFixedCommits,
        packageHomePage,
        packageEdition,
        affectedVersions,
        ranges,
        rangesType,
        rangesRepo,
        rangesEvents,
        rangesDatabaseSpecific,
        affectedEcosystemSpecific,
        affectedDatabaseSpecific,
        patchesDetail,
        patchesPatchUrl,
        patchesIssueUrl,
        patchesMainLanguage,
        patchesAuthor,
        patchesCommiter,
        patchesBranches,
        patchesTags
)