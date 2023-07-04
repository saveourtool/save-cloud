/**
 * InputForms additionally contains data class Popover
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.inputform

import com.saveourtool.save.validation.*

private const val URL_PLACEHOLDER = "https://example.com"
private const val EMAIL_PLACEHOLDER = "test@example.com"

private const val NAME_TOOLTIP = "Allowed symbols: letters, digits, dots, hyphens and underscores." +
        "No dot, hyphen or underscore at the beginning and at the end of the line."

/**
 * @property str
 * @property placeholder
 * @property errorMessage
 * @property tooltip
 * @property popover
 */
@Suppress("WRONG_DECLARATIONS_ORDER")
enum class InputTypes(
    val str: String,
    val errorMessage: String? = null,
    val placeholder: String? = null,
    val tooltip: String? = null,
    val popover: Popover? = null,
) {
    // ==== general
    DESCRIPTION("description", null, "description"),
    COMMENT("comment", null, "comment"),

    // ==== new project view
    // TODO: need to removed or move to new modal window
    GIT_BRANCH("git branch", null, placeholder = "leave empty if you would like to use default branch"),
    GIT_TOKEN("git token", null, "token"),
    GIT_URL("git url", URL_ERROR_MESSAGE, URL_PLACEHOLDER),
    GIT_USER("git username", null, "username"),
    PROJECT_EMAIL("project email", EMAIL_ERROR_MESSAGE, EMAIL_PLACEHOLDER),
    PROJECT_PROBLEM_NAME("project problem name", NAME_ERROR_MESSAGE, placeholder = "name"),

    // ==== signIn view
    LOGIN(
        "login",
        null,
        "login",
        NAME_TOOLTIP
    ),
    PASSWORD("password", null, "*****"),
    PROJECT_NAME(
        "project name",
        NAME_ERROR_MESSAGE,
        "name",
        NAME_TOOLTIP
    ),
    PROJECT_URL("project Url", URL_ERROR_MESSAGE, URL_PLACEHOLDER),
    PROJECT_VERSION("project Url", placeholder = "0.0.1; 0.0.3-0.0.5; 1.0.1.RELEASE; etc."),

    // ==== create organization view
    ORGANIZATION_NAME(
        "organization name",
        NAME_ERROR_MESSAGE,
        "name",
        NAME_TOOLTIP
    ),

    // ==== user setting view
    USER_EMAIL("user email", EMAIL_ERROR_MESSAGE, EMAIL_PLACEHOLDER),
    USER_NAME("user name", NAME_ERROR_MESSAGE, "name"),
    COMPANY("company"),
    LOCATION("location"),
    GIT_HUB("git hub"),
    LINKEDIN("linkedin"),
    TWITTER("twitter"),

    // ==== contest creation component
    CONTEST_NAME(
        "contest name",
        NAME_ERROR_MESSAGE,
        "name",
        NAME_TOOLTIP
    ),
    CONTEST_TEMPLATE_NAME(
        "Contest template name",
        NAME_ERROR_MESSAGE,
        "name",
        NAME_TOOLTIP,
    ),
    CONTEST_START_TIME("contest starting time", DATE_RANGE_ERROR_MESSAGE),
    CONTEST_END_TIME("contest ending time", DATE_RANGE_ERROR_MESSAGE),
    CONTEST_DESCRIPTION("contest description"),
    CONTEST_SUPER_ORGANIZATION_NAME("contest's super organization's name", NAME_ERROR_MESSAGE),
    CONTEST_TEST_SUITE_IDS("contest test suite ids", placeholder = "click to open selector"),

    // ==== test suite source creation
    SOURCE_NAME("source name", placeholder = "name", tooltip = NAME_TOOLTIP),
    SOURCE_GIT("source git"),
    SOURCE_TEST_ROOT_PATH(
        "test root path",
        placeholder = "leave empty if tests are in repository root",
        tooltip = "Relative path to the root directory with tests",
        popover = Popover(
            title = "Relative path to the root directory with tests",
            content = """
                The path you are providing should be relative to the root directory of your repository.
                This directory should contain <a href = "https://github.com/saveourtool/save#how-to-configure"> save.properties </a>
                or <a href = "https://github.com/saveourtool/save-cli#-savetoml-configuration-file">save.toml</a> files.
                For example, if the URL to your repo with tests is: 
                <a href ="https://github.com/saveourtool/save-cli/">https://github.com/saveourtool/save</a>, then
                you need to specify the following directory with 'save.toml': 
                <a href ="https://github.com/saveourtool/save-cli/tree/main/examples/kotlin-diktat">examples/kotlin-diktat/</a>.
            """
        )
    ),

    // ==== test suites source fetcher
    SOURCE_TAG("source_tag", placeholder = "select a tag"),
    SOURCE_BRANCH("source_branch", placeholder = "select a branch"),
    SOURCE_COMMIT("source_commit", placeholder = "select a commit"),

    // ==== execution run
    TEST_SUITE_IDS("test suite ids", placeholder = "click to open selector"),

    // ==== ace editor
    ACE_THEME_SELECTOR("theme"),
    ACE_MODE_SELECTOR("mode"),

    // ==== vulnerability
    CVE_NAME(
        "CVE identifier",
        CVE_NAME_ERROR_MESSAGE,
        placeholder = "CVE-2023-######, etc.",
        tooltip = "If you know the vulnerability identifier, you can enter it here",
    ),
    CVE_DATE("CVE date"),
    ;
}

/**
 * Data class to store popover values in a single object
 *
 * @property title
 * @property content
 */
data class Popover(
    val title: String,
    val content: String,
)
