package com.saveourtool.save.utils.github

/**
 * @property organizationName
 * @property projectName
 */
class GitHubRepo(
    var organizationName: String,
    var projectName: String,
) {
    /**
     * @param tagName
     * @return URL to request metadata for provided [tagName]
     */
    fun getMetadataUrl(tagName: String): String = if (tagName == GitHubHelper.LATEST_VERSION) {
        tagName
    } else {
        "tags/$tagName"
    }
        .let { release ->
            "${GitHubHelper.API_URL}/$organizationName/$projectName/releases/$release"
        }

    /**
     * @return URL to request tags
     */
    fun getTagsUrl(): String = "${GitHubHelper.API_URL}/$organizationName/$projectName/tags"
}
